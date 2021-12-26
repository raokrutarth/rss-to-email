package fyi.newssnips.datacruncher

import org.apache.spark.sql._
import fyi.newssnips.models._
import fyi.newssnips.models.FeedContent
import fyi.newssnips.shared.DateTimeUtils
import com.typesafe.scalalogging.Logger
import java.net.URL
import scala.concurrent._
import java.util.concurrent.Executors
import fyi.newssnips.datacruncher.core.Scraper

object ScratchCode {
  private val log = Logger("app." + this.getClass().toString())
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  val us =
    Seq(
      "https://www.reddit.com/r/stocks/hot/.rss",
      "https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx6TVdZU0FtVnVHZ0pWVXlnQVAB?hl=en-US&gl=US&ceid=US%3Aen&oc=11",
      "https://www.reddit.com/r/tgif/.rss",
      "https://www.reddit.com"
    )

  // "https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx6TVdZU0FtVnVHZ0pWVXlnQVAB"
  // "https://slate.com/feeds/all.rss"
  // "https://api.axios.com/feed/"

  log.info("Starting scrape")
  val feeds =
    us.groupBy(u => new URL(u).getHost()).flatMap { case (host, urls) =>
      log
        .info(s"running scrape for host: $host, Urls: ${urls.mkString(", ")}")

      // TODO make below parallel for all hosts.
      urls.map { u =>
        Scraper.getAndParseFeed(FeedURL(u), false) match {
          case Some(f) =>
            Thread.sleep(750)
            f
          case _ => None
        }
      }
    }
  log.info(s"feeds content: ${feeds.size}")

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("newssnips.fyi")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .master("local[*]")
      .getOrCreate()

  val feed = Feed(
    title = "test",
    url = FeedURL("gfeed"),
    content = Seq(
      FeedContent(
        url =
          "https://news.google.com/__i/rss/rd/articles/CBMiK2h0dHBzOi8vd3d3LnlvdXR1YmUuY29tL3dhdGNoP3Y9cXZ2Nzc0N05NQ3PSAQA?oc=5",
        title =
          "Jim Cramer sees 2 possible 'speed bumps' for stocks. Here's how he is preparing - CNBC Television",
        body = ""
      )
    ),
    lastScraped = DateTimeUtils.now()
  )
  val dprep = new DataPrep(spark)

  dprep.constructContentsDf(feed.content)

  spark.stop
}
