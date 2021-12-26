package fyi.newssnips.datacruncher

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import fyi.newssnips.models.{AnalysisRow, Feed, FeedContent, FeedURL}
import fyi.newssnips.datacruncher.core.{Analysis, Scraper}
import scala.util.{Failure, Success, Try}
import datastore.DocumentStore
import fyi.newssnips.datastore.DatastaxCassandra

import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

object AnalysisCycle {
  def f() = {
    val log = Logger(this.getClass())

    val feeds = Seq(
      "http://rss.cnn.com/rss/cnn_topstories.rss",
      "http://rss.cnn.com/rss/cnn_world.rss",
      "http://rss.cnn.com/rss/cnn_us.rss",
      "http://rss.cnn.com/rss/cnn_latest.rss",
      "https://rss.politico.com/congress.xml",
      "http://rss.politico.com/politics.xml",
      "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
      "http://feeds.feedburner.com/zerohedge/feed",
      "http://thehill.com/rss/syndicator/19110",
      "http://thehill.com/taxonomy/term/1778/feed",
      "https://nypost.com/feed",
      "https://snewsi.com/rss",
      "https://feeds.simplecast.com/54nAGcIl",
      "https://www.reddit.com/r/StockMarket/.rss"
    )
    // FIXME: at scale, ideal to write contents from each feed to
    // S3 and let the spark streaming watcher ingest them in betches.
    val allContents: Seq[Seq[FeedContent]] = feeds.flatMap { u =>
      Thread.sleep(1000)
      Scraper.getContent(FeedURL(u))
    }

    // log.info(s"$allContents")

    // val allContents = List(
    //   List(
    //     FeedContent(
    /* "https://www.wsj.com/articles/u-s-military-chief-says-chinas-hypersonic-missile-test-is-close-to-sputnik-moment-11635344992", */
    /* "China's Hypersonic Missile Test Is Close to 'Sputnik Moment,' U.S. Military Chief Says", */
    /* "Gen. Mark Milley described China’s recent test of a hypersonic missile as “very concerning”
     * and said the Pentagon was focused on the development.", */
    //       false
    //     ),
    //     FeedContent(
    /* "https://www.wsj.com/articles/iran-to-return-to-nuclear-deal-talks-in-vienna-next-month-11635348645", */
    //       "Iran to Return to Nuclear Deal Talks Next Month",
    /* "Tehran will return to negotiations on reviving the 2015 nuclear deal by the end of November,
     * its chief negotiator said Wednesday.", */
    //       false
    //     ),
    //     FeedContent(
    /* "https://www.wsj.com/articles/iran-to-return-to-nuclear-deal-talks-in-vienna-next-month-11635348645", */
    //       "Iran to Return to Nuclear Deal Talks Next Month",
    /* "Tehran will return to negotiations on reviving the 2015 nuclear deal by the end of November,
     * its chief negotiator said Wednesday.", */
    //       false
    //     )
    //   )
    // )
    val analysis = new Analysis()
    val reportRows =
      analysis.generateReport(allContents.flatten)

    val db = new DocumentStore()

    db.upsertAnalysis("home.page.analysis.rows", reportRows) match {
      case Failure(s) =>
        log.error(s"Failed to store analysis. Reason: $s")
      case Success(_) =>
        log.info(s"Home page analysis rows svaed successfully.")
    }

    // memory info
    val mb      = 1024 * 1024
    val runtime = Runtime.getRuntime
    println(
      "** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb + " MB"
    )
    println("** Max Memory:   " + runtime.maxMemory / mb)
    analysis.cleanup()
  }
}

object Main extends App {
  val log = Logger(this.getClass())

  // DatastaxCassandra.upsertKV("test", "999") match {
  //   case Failure(s) => println(s)
  //   case _          => log.info("done")
  // }
  // println(DatastaxCassandra.getKV("test"))
  // DatastaxCassandra.deleteKV("test") match {
  //   case Failure(s) => println(s)
  //   case _          => log.info("done")
  // }
  import DatastaxCassandra.spark.implicits._

  val k1 = "mtinittable"
  val df1 = Seq(
    ("b00001", "Sir Arthur Conan Doyle", "A3 study in scarlet", 1887),
    ("b00023", "Sir Arthur Conan Doyle", "A sign of four", 1890)
  ).toDF("book_id", "book_author", "book_name", "book_pub_year")
  df1.show()

  DatastaxCassandra.putDataframe(k1, df1, "book_id") match {
    case Failure(s) => println(s)
    case _          => log.info("df1 added")
  }

  val df2: DataFrame = DatastaxCassandra.getDataframe(k1) match {
    case Success(d) =>
      d.show
      d
    case _ => {
      log.error("unable to get df2")
      Seq(0).toDF()
    }
  }

  df2.persist()
  val df3 = df2
    .withColumn("x4", col("book_pub_year") * 100)
    .limit(1)
  df3.show()

  DatastaxCassandra.deleteDataframe(k1) match {
    case Failure(s) => println(s)
    case _          => log.info("df removed")
  }

  DatastaxCassandra.putDataframe(
    k1,
    df3,
    "book_id"
  ) match {
    case Failure(s) => println(s)
    case _          => log.info("df3 added")
  }

  DatastaxCassandra.getDataframe(k1) match {
    case Success(d) =>
      log.info("d3:")
      d.count()
      d.show
    case _ => {
      log.error("unable to get updated df")
    }
  }

}
