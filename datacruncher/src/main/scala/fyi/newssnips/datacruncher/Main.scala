package fyi.newssnips.datacruncher

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import fyi.newssnips.datacruncher.models.{Feed, FeedURL, FeedContent, AnalysisRow}
import fyi.newssnips.datacruncher.core.{Scraper, Analysis}

object Main extends App {
  val log = Logger(this.getClass())

  // val conf = new SparkConf()
  //   .setMaster("local")
  //   .setAppName("my awesome app")
  // val sc = new SparkContext(conf)
  val analysis = new Analysis()
val allContents: Seq[Seq[FeedContent]] = Seq(
          // "http://rss.cnn.com/rss/cnn_topstories.rss"
          // "http://rss.cnn.com/rss/cnn_world.rss",
          // "http://rss.cnn.com/rss/cnn_us.rss"
          // "http://rss.cnn.com/rss/cnn_latest.rss"
          // "https://rss.politico.com/congress.xml",
          // "http://rss.politico.com/politics.xml"
          // "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
          // "http://feeds.feedburner.com/zerohedge/feed",
          // "http://thehill.com/rss/syndicator/19110",
          // "http://thehill.com/taxonomy/term/1778/feed",
          // "https://nypost.com/feed"
          // "https://snewsi.com/rss"
          "https://feeds.simplecast.com/54nAGcIl"
          // "https://www.reddit.com/r/StockMarket/.rss"
        )
          .flatMap(u => Scraper.getContent(FeedURL(u)))

        val reportRows =
          analysis.generateReport(allContents.flatten)

        // db.upsertAnalysis("home.page.analysis.rows", reportRows) match {
        //   case Failure(s) =>
        //     log.error(s"Failed to store analysis. Reason: $s")
        //   case Success(_) =>
        //     log.info(s"Home page analysis rows svaed successfully.")
        // }

        // memory info
        val mb = 1024 * 1024
        val runtime = Runtime.getRuntime
        println(
          "** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb + " MB"
        )
        println("** Max Memory:   " + runtime.maxMemory / mb)
      analysis.cleanup()
}
