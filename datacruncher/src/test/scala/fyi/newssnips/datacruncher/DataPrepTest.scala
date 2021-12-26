package fyi.newssnips.datacruncher.test

import org.scalatest._
import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.SparkSession
import fyi.newssnips.datacruncher.NerHelper
import org.apache.spark.sql.functions._
import fyi.newssnips.models._
import fyi.newssnips.datacruncher._
import fyi.newssnips.shared._

/*
TODO
===
- https://www.baeldung.com/scala/scalatest
- https://www.scalatest.org/user_guide/sharing_fixtures
 */
class DataPrepTestSpec extends FlatSpec with BeforeAndAfter {
  val log = Logger("app." + this.getClass().toString())

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

  after {
    spark.stop()
  }

  "entity names" should "be normalized" in {
    val enDf = Seq(
      ("U.S."),
      ("U.S"),
      ("General Electric"),
      ("GE"),
      ("the U.S."),
      ("US"),
      ("Unites States"),
      ("Andrew Err's"),
      ("Supreme Court’s"),
      ("Jeffrey Epstein’s")
    ).toDF("entityName")
    enDf.show(false)

    enDf
      .withColumn(
        "entityName",
        NerHelper.entityNameNormalizeUdf(col("entityName"))
      )
      .show(false)
    // TODO check conditions
  }

  "gnews titles" should "be normalized" in {
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
  }
}
