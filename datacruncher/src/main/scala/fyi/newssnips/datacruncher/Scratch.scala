package fyi.newssnips.datacruncher

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
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

  def checkCaseFix() = {
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
    import spark.implicits._
    log.info("Running manual text normalize tests.")
    val enUdf = NerHelper.entityNameNormalizeUdf
    val df = Seq(
      "EU",
      "u.k",
      "omi",
      "Omi",
      "ted, j",
      "ted js",
      "Ted j",
      "kku lio yyu",
      "p",
      "United States of America",
      "U.S.A",
      " the orbit"
    ).toDF("text")
      .withColumn("initCap", initcap(col("text")))
      .withColumn("eName", enUdf(col("text")))
      .withColumn("trimPunct", regexp_replace(col("text"), "(\\W+$|^\\W+)", ""))
    df.show(false)
    spark.stop
  }
  checkCaseFix()

  def negOverride() = {
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

    import spark.implicits._
    val negTexts = ManualOverrides.negativePhrases
    // val q        = s"""lower('text') contains ${negTexts.mkString(" OR ")}"""
    val df = Seq(
      (
        "COVID-19 Hospitalizations rise in California amid omicron surge",
        "pos"
      ),
      ("Actor GH Tubby dies at age 45.", "pos"),
      ("Scooters are nice.", "pos")
    ).toDF("text", "sentiment")
      .withColumn(
        "sentimentOvr",
        when(negTexts.map(lower(col("text")).contains).reduce(_ || _), "neg")
          .otherwise(col("sentiment"))
      )

    df.show(false)
    spark.stop
  }

  log.info("scratch finished execution.")
}
