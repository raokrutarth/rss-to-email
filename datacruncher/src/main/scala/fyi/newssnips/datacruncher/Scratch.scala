package fyi.newssnips.datacruncher

import org.apache.spark.sql._
import com.typesafe.scalalogging.Logger
import scala.concurrent._
import java.util.concurrent.Executors

object ScratchCode {
  private val log = Logger("app." + this.getClass().toString())
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  val us =
    Seq(
      // "https://www.reddit.com/r/stocks/hot/.rss",
      "https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx6TVdZU0FtVnVHZ0pWVXlnQVAB?hl=en-US&gl=US&ceid=US%3Aen&oc=11"
      // "https://www.reddit.com/r/tgif/.rss"
    )

  // "https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx6TVdZU0FtVnVHZ0pWVXlnQVAB"
  // "https://slate.com/feeds/all.rss"
  // "https://api.axios.com/feed/"

  val f = CycleHelpers.extractFeeds("h", us)
  log.info(s"Extracted feeds: ${f.head.content(0)}")

  def sp() = {
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

    spark.stop
  }

}
