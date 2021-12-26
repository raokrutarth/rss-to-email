package fyi.newssnips.datacruncher

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql._
import fyi.newssnips.models.FeedURL
import fyi.newssnips.datacruncher.core.Scraper

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import fyi.newssnips.shared.DbConstants
import fyi.newssnips.shared.DateTimeUtils

import scala.util._
import fyi.newssnips.datacruncher.core.Analysis

import fyi.newssnips.models.Feed
import fyi.newssnips.datastore.Cache
import org.apache.spark.storage.StorageLevel
import fyi.newssnips.datacruncher.datastore.SparkPostgres
import configuration.AppConfig
import java.net.URL

object CycleHelpers {
  private val log = Logger("app." + this.getClass().toString())
  /*
    RSS feed URL to feed obj.
    pause only if there is more urls to scrape
   */
  def extractFeeds(host: String, urls: Seq[String]): Seq[Feed] = {
    log.info(
      s"Running scrape for host: $host with ${urls.size} feed URL(s)."
    )

    var curr  = urls
    var feeds = Seq[Feed]()

    while (curr.nonEmpty) {
      Scraper.getAndParseFeed(FeedURL(curr.head), false) match {
        case Some(f) => feeds = feeds :+ f
        case _       => log.error(s"Unable to extract feed from $curr.")
      }
      curr = curr.tail
      if (curr.nonEmpty) Thread.sleep(1000)
    }
    feeds
  }
}

object AnalysisCycle {
  private val log = Logger("app." + this.getClass().toString())

  lazy val spark: SparkSession =
    SparkSession
      .builder()
      .appName("newssnips.fyi")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .config("spark.sql.broadcastTimeout", 1200)
      .master("local[*]")
      .getOrCreate()

  import spark.implicits._

  private val db = new SparkPostgres(spark)

  private val cache = new Cache()

  private lazy val analysis = new Analysis(spark)
  log.info("Initalized analysis module.")

  private val dataPrep = new DataPrep(spark)
  log.info("Initalized dataprep module.")

  private val booksFinderUdf = udf(ContextFinder.findBooks)

  private def categoryCycle(categoryId: String, categoryFeeds: Seq[Feed]) = {
    val categoryMetadata = DbConstants.categoryToDbMetadata(categoryId)

    // create feeds df
    val feedsDf: DataFrame = categoryFeeds.zipWithIndex
      .map { case (f, i) =>
        (i, f.title, f.url.value, DateTimeUtils.getDateAsString(f.lastScraped))
      }
      .toDF("feed_id", "title", "url", "last_scraped")

    log.info(s"Feeds for category ${categoryId}:")
    feedsDf.show(false) // small table. ok to print.

    val categoryContents = categoryFeeds.map { f => f.content }.flatten

    val fatContentsDf       = dataPrep.constructContentsDf(categoryContents)
    val (urlDf, contentsDf) = dataPrep.seperateLinksFromContents(fatContentsDf)

    // metadata if the report generation is successful. (df, tableName, idCol)
    val toSave: Seq[(DataFrame, String)] = Seq(
      (feedsDf, categoryMetadata.sourceFeedsTableName),
      (urlDf, categoryMetadata.articleUrlsTableName),
      (contentsDf, categoryMetadata.textsTableName)
    )
    contentsDf.persist(StorageLevel.DISK_ONLY)

    val reportDf = analysis.generateReport(contentsDf)

    reportDf match {
      case Some(df) =>
        log.info("Fetching context products for all entities in report.")

        val resDf = df
          .withColumn(
            "contextBooks",
            booksFinderUdf(col("entityName"), col("entityType"))
          )

        log.info(s"Final analysis report for $categoryId:")
        resDf.persist(StorageLevel.DISK_ONLY)
        resDf.show() // critical dataframe to see before saving

        db.putDataframe(
          categoryMetadata.analysisTableName,
          resDf
        ) match {
          case Failure(s) =>
            log.error(s"Failed to store analysis. Reason: $s")
          case _ =>
            resDf.unpersist()
            log.info(
              s"${categoryMetadata.analysisTableName} rows saved successfully. Saving analysis metadata."
            )
            toSave.foreach { case (df, tableName) =>
              db.putDataframe(tableName, df) match {
                case Failure(exception) =>
                  log.error(
                    s"Failed to save table $tableName with error $exception"
                  )
                case _ =>
                  log.info(s"Saved metadata table $tableName")
              }
            }
            db.upsertKV(
              categoryMetadata.lastUpdateKey,
              DateTimeUtils.getDateAsStringUi(DateTimeUtils.now())
            ) match {
              case Failure(_) =>
                log.error(
                  s"Failed to set updated-at time for ${categoryMetadata.lastUpdateKey}"
                )
              case _ =>
                log.info(
                  s"$categoryId page analysis and metadata successfully saved."
                )
            }
        }
      case _ => log.error("Unable to generate home page results report.")
    }
    contentsDf.unpersist()
  }

  for ((categoryId, urls) <- FeedUrls.categoryToUrls) {
    log.info(
      s"Generating feeds for category $categoryId with ${urls.size} URL(s)."
    )

    // se only one feed in developemnt/test mode
    val cycleUrls =
      // shuffeling reduces rate limit errors
      if (AppConfig.settings.shared.inProd) Random.shuffle(urls.distinct)
      else Random.shuffle(urls).take(2)

    val categoryFeeds: Seq[Feed] =
      cycleUrls
        .groupBy(u => new URL(u).getHost())
        .flatMap { case (host, urls) =>
          CycleHelpers.extractFeeds(host, urls) // TODO can run this in parallel
        }
        .toSeq

    categoryCycle(categoryId, categoryFeeds)
  }

  cache.flushCache()
  cache.cleanup()
  spark.stop()
}
