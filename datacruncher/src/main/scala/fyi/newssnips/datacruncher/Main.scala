package fyi.newssnips.datacruncher

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import fyi.newssnips.models.{AnalysisRow, Feed, FeedContent, FeedURL}
import fyi.newssnips.datacruncher.core.{Analysis, Scraper}
import scala.util.{Failure, Success, Try}
import fyi.newssnips.datastore.DatastaxCassandra

import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

object AnalysisCycle {
  val log = Logger(this.getClass())

  val feeds = Seq(
    // "http://rss.cnn.com/rss/cnn_topstories.rss",
    // "http://rss.cnn.com/rss/cnn_world.rss",
    // "http://rss.cnn.com/rss/cnn_us.rss",
    // "http://rss.cnn.com/rss/cnn_latest.rss",
    // "https://rss.politico.com/congress.xml",
    // "http://rss.politico.com/politics.xml",
    // "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
    // "http://feeds.feedburner.com/zerohedge/feed",
    // "http://thehill.com/rss/syndicator/19110",
    // "http://thehill.com/taxonomy/term/1778/feed",
    // "https://nypost.com/feed",
    // "https://snewsi.com/rss",
    // "https://feeds.simplecast.com/54nAGcIl",
    "https://www.reddit.com/r/StockMarket/.rss"
  )
  // FIXME: at scale, ideal to write contents from each feed to
  // S3 and let the spark streaming watcher ingest them in batches.
  val allContents: Seq[Seq[FeedContent]] = feeds.flatMap { u =>
    Thread.sleep(1000)
    Scraper.getContent(FeedURL(u))
  }

  val db = DatastaxCassandra

  val analysis = new Analysis(db.spark)

  analysis.generateReport(allContents.flatten) match {
    case Some(df) =>
      db.putDataframe(
        "home_page_analysis_results",
        df,
        col("entityName"),
        col("entityType")
      ) match {
        case Failure(s) =>
          log.error(s"Failed to store analysis. Reason: $s")
        case Success(_) =>
          log.info(s"Home page analysis rows saved successfully.")
      }
    case _ => log.error("Unable to generate home page results report.")
  }

  db.cleanup()
}

object Main extends App {
  val log = Logger(this.getClass())

  AnalysisCycle
}
