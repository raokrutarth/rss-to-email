package fyi.newssnips.datacruncher

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql._
import fyi.newssnips.models.FeedURL
import fyi.newssnips.datacruncher.core.Scraper
import fyi.newssnips.datastore.DatastaxCassandra

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import fyi.newssnips.shared.DbConstants
import fyi.newssnips.shared.{DateTimeUtils, DfUtils}

import scala.util.Failure
import fyi.newssnips.datacruncher.core.Analysis

object AnalysisCycle {
  private val log = Logger(this.getClass())

  private val db = DatastaxCassandra
  import db.spark.implicits._
  private val analysis = new Analysis(db.spark)
  private val dataPrep = new DataPrep(db.spark)

  private val booksFinderUdf = udf(ContextFinder.findBooks)
  private val talksFinderUdf = udf(ContextFinder.findTalks)

  val categoryToUrls: Map[String, Seq[String]] = Map(
    "home" -> Seq(
      "https://feeds.a.dj.com/rss/RSSWorldNews.xml"
    ),
    "markets" -> Seq(
      "https://www.reddit.com/r/StockMarket/rising/.rss",
      "http://feeds.marketwatch.com/marketwatch/realtimeheadlines/"
    ),
    "politics" -> Seq(
      "https://rss.politico.com/congress.xml",
      "http://thehill.com/rss/syndicator/19110"
    ),
    "entertainment" -> Seq(
      "https://www.buzzfeed.com/celebrity.xml",
      "https://www.buzzfeed.com/tvandmovies.xml"
    )
  )

  for ((categoryId, urls) <- categoryToUrls) {
    log.info(
      s"Generating feeds for category $categoryId with ${urls.size} URL(s)."
    )
    val categoryMetadata = DbConstants.categoryToDbMetadata(categoryId)

    val feedsDf: DataFrame = db.spark.sparkContext
      .parallelize(urls.zipWithIndex)
      .toDF("url", "feed_id")
    DfUtils.showSample(feedsDf, 5f)

    val categoryContents = urls.flatMap { u =>
      Thread.sleep(500)
      Scraper.getContent(FeedURL(u))
    }.flatten

    val fatContentsDf       = dataPrep.constructContentsDf(categoryContents)
    val (urlDf, contentsDf) = dataPrep.seperateLinksFromContents(fatContentsDf)

    contentsDf.cache()
    // metadata if the report generation is successful. (df, tableName, idCol)
    val toSave: Seq[(DataFrame, String, String)] = Seq(
      (feedsDf, categoryMetadata.sourceFeedsTableName, "feed_id"),
      (urlDf, categoryMetadata.articleUrlsTableName, "link_id"),
      (contentsDf, categoryMetadata.textsTableName, "text_id")
    )

    analysis.generateReport(contentsDf) match {
      case Some(df) =>
        log.info("Fetching context paterials for all entities in report.")

        val resDf = df
          .withColumn(
            "contextBooks",
            booksFinderUdf(col("entityName"), col("entityType"))
          )
          .withColumn(
            "contextTalks",
            talksFinderUdf(col("entityName"), col("entityType"))
          )

        log.info("Fetched context items for all entities in report.")
        resDf.show()

        db.putDataframe(
          categoryMetadata.analysisTableName,
          resDf,
          col("entityName"),
          col("entityType")
        ) match {
          case Failure(s) =>
            log.error(s"Failed to store analysis. Reason: $s")
          case _ =>
            log.info(
              s"Home page analysis rows saved successfully. Saving analysis metadata."
            )
            toSave.foreach { case (df, tableName, idCol) =>
              db.putDataframe(
                tableName,
                df,
                col(idCol)
              ) match {
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
              DateTimeUtils.getDateAsString(DateTimeUtils.now())
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
  }

  db.cleanup()
}

object Main extends App {
  val log = Logger(this.getClass())
  AnalysisCycle
}
