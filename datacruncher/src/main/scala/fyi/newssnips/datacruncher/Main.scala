package fyi.newssnips.datacruncher

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql._
import fyi.newssnips.models.FeedURL
import fyi.newssnips.datacruncher.core.Scraper
import fyi.newssnips.datastore.DatastaxCassandra

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import fyi.newssnips.shared.DbConstants
import fyi.newssnips.shared.DateTimeUtils

import scala.util.Failure
import fyi.newssnips.datacruncher.core.Analysis

import fyi.newssnips.models.Feed
import fyi.newssnips.datastore.Cache

object AnalysisCycle {
  private val log = Logger("app." + this.getClass().toString())

  private val db = DatastaxCassandra
  import db.spark.implicits._
  private val cache = new Cache()

  private val analysis = new Analysis(db.spark)
  log.info("Initalized analysis module.")

  private val dataPrep = new DataPrep(db.spark)
  log.info("Initalized dataprep module.")

  private val booksFinderUdf = udf(ContextFinder.findBooks)

  val categoryToUrls: Map[String, Seq[String]] = Map(
    "home" -> Seq(
      "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
      "https://www.reddit.com/r/UpliftingNews/.rss",
      "https://www.reddit.com/r/worldnews/.rss",
      "http://rss.cnn.com/rss/cnn_world.rss",
      "http://rss.cnn.com/rss/cnn_us.rss",
      "http://rss.cnn.com/rss/cnn_latest.rss",
      "https://nypost.com/feed",
      "http://feeds.feedburner.com/zerohedge/feed",
      "https://www.huffpost.com/section/front-page/feed?x=1",
      "http://feeds.foxnews.com/foxnews/latest",
      "http://feeds.foxnews.com/foxnews/world",
      "https://cdn.feedcontrol.net/8/1114-wioSIX3uu8MEj.xml",
      "https://www.yahoo.com/news/rss",
      "https://www.rt.com/rss/news/"
    ),
    "markets" -> Seq(
      "http://feeds.marketwatch.com/marketwatch/realtimeheadlines/",
      "https://finance.yahoo.com/news/rss",
      "https://www.nasdaq.com/feed/rssoutbound",
      "https://seekingalpha.com/market_currents.xml",
      "https://seekingalpha.com/feed.xml",
      "http://feeds.feedburner.com/TradingVolatility",
      "http://rss.politico.com/economy.xml",
      "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
      "http://thehill.com/taxonomy/term/30/feed",
      "http://thehill.com/taxonomy/term/20/feed",
      "https://nypost.com/business/feed/",
      "http://feeds.marketwatch.com/marketwatch/topstories/",
      "https://fool.libsyn.com/rss",
      "https://www.cnbc.com/id/20409666/device/rss/rss.html?x=1",
      "https://www.wallstreetsurvivor.com/feed/",
      "https://www.investing.com/rss/news_25.rss",
      "https://www.reddit.com/r/StockMarket/.rss",
      "https://www.reddit.com/r/wallstreetbets/.rss",
      "https://www.reddit.com/r/stocks/.rss"
    ),
    // "technology" -> Seq("https://www.techmeme.com/feed.xml"),
    "politics" -> Seq(
      "https://rss.politico.com/congress.xml",
      "http://thehill.com/rss/syndicator/19110",
      "https://www.memeorandum.com/feed.xml",
      "https://www.reddit.com/r/politics/.rss",
      "https://www.reddit.com/r/NeutralPolitics/.rss",
      "https://news.yahoo.com/rss/politics"
    ),
    "entertainment" -> Seq(
      "https://www.buzzfeed.com/celebrity.xml",
      "https://www.buzzfeed.com/tvandmovies.xml",
      "http://syndication.eonline.com/syndication/feeds/rssfeeds/topstories.xml",
      "https://meredith.mediaroom.com/news-releases?pagetemplate=rss&category=816",
      "http://feeds.bet.com/AllBetcom",
      "https://www.hollywoodintoto.com/feed/",
      "https://hollywoodlife.com/feed/",
      "https://mtonews.com/.rss/full/",
      "http://feeds.feedburner.com/variety/headlines",
      "https://www.reddit.com/r/entertainment/.rss",
      "https://www.reddit.com/r/celebrities/.rss",
      "https://www.reddit.com/r/entertainment/.rss",
      "https://www.wesmirch.com/feed.xml",
      "https://mediagazer.com/feed.xml",
      "https://www.yahoo.com/entertainment/rss"
    )
  )

  for ((categoryId, urls) <- categoryToUrls) {
    log.info(
      s"Generating feeds for category $categoryId with ${urls.size} URL(s)."
    )
    val categoryMetadata = DbConstants.categoryToDbMetadata(categoryId)

    val categoryFeeds: Seq[Feed] = urls.flatMap { u =>
      // TODO group urls by host/first8-chars and reduce sleep time.
      Thread.sleep(726) // sleep to avoid rate limiting
      Scraper.getAndParseFeed(FeedURL(u))
    }

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

        log.info(s"Final analysis report for $categoryId:")
        resDf.show() // critical dataframe to see before saving

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
              s"${categoryMetadata.analysisTableName} rows saved successfully. Saving analysis metadata."
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
    contentsDf.unpersist()
  }
  cache.flushCache()
  cache.cleanup()
  db.cleanup()
}

object Main extends App {
  val log = Logger("app." + this.getClass().toString())
  log.info("Running analysis cycle.")
  // AnalysisCycle
  Sentiment1
  log.info("Analysis cycle finished.")
}
