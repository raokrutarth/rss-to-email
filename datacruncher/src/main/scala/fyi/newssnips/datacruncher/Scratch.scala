package fyi.newssnips.datacruncher

import org.apache.spark.sql._
import com.typesafe.scalalogging.Logger
import scala.concurrent._
import java.util.concurrent.Executors
import com.github.ghostdogpr.readability4s.Readability
import configuration.AppConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils

object ScratchCode {
  private val log = Logger("app." + this.getClass().toString())
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  def some() = {
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
  }

  def foo() = {

    val httpClient =
      HttpClientBuilder
        .create()
        .setDefaultRequestConfig(AppConfig.settings.httpClientConfig)
        .build()

    val request = new HttpGet(
      "https://www.schwab.com/resource-center/insights/content/schwab-market-update"
    )
    request.setHeader(
      "user-agent",
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.2 Safari/605.1.15"
    )
    request.setHeader(
      "Accept",
      "application/rss+xml, application/xml, application/atom+xml, text/xml" // application/rdf+xml
    )
    val response   = httpClient.execute(request)
    val statusCode = response.getStatusLine().getStatusCode()
    log.info(s"$statusCode")

    val payload = EntityUtils.toString(response.getEntity())

    val aOpt = Readability(
      "https://www.schwab.com/resource-center/insights/content/schwab-market-update",
      payload
    ).parse()
    aOpt match {
      case Some(a) => {
        println(a.title)
        println(a.textContent)
        println(a.excerpt)
      }
      case _ => log.error("Parsing failed.")
    }
  }
  foo()

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
