package fyi.newssnips.datacruncher.scripts

import org.apache.spark.sql.SparkSession
import com.typesafe.scalalogging.Logger
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.SparkSession
import com.typesafe.scalalogging.Logger
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.ml._

object ModelExpriments {
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

  def sentiment() = {

    lazy val exampleDf = {
      log.info(s"Sample contents df:")
      val df = Seq(
        (
          1,
          "Beijing Furious After Biden Invites Taiwan To Global Democracy Summit, " +
            "while China Left Off List. the want to talk again.",
          "pos"
        ),
        (
          12,
          "Kim Kardashian's Romance With Pete Davidson Is a 'Positive Transition for Her'.",
          "pos"
        ),
        (
          122,
          "As the U.S. and Russia talk, Ukraine braces for another invasion.",
          "neg"
        ),
        (
          1224,
          "U.S. judge refuses to dismiss civil sex abuse case against Prince Andrew.",
          "neg"
        ),
        (
          12243,
          "Andrew drove into a wall.",
          "neg"
        ),
        (
          122432,
          "Spain refuses the British treaty on coffee beans.",
          "neg"
        )
      ).toDS.toDF("text_id", "text", "actual")
      df.show()
      df
    }

    val sentimentPipelineSocial = {
      log.info("Initalizing sentiment pipeline.")
      PipelineModel.read.load(ModelStore.sentimentModelPath.toString())
    }
    val transformed = sentimentPipelineSocial.transform(exampleDf)
    transformed.printSchema()

    // val isNum = udf((value: String) => Try(value.toInt).isSuccess)
    val preAgg = transformed
      .select(
        col("text_id"),
        col("actual"),
        explode(col("sentiment")).as("sentiment_full")
      )
    preAgg.show(false)

    val sentimentDf = preAgg.select(
      col("text_id"),
      col("sentiment_full.result").as("label"),
      col("actual"),
      array_max(
        map_values($"sentiment_full.metadata").cast(ArrayType(DoubleType))
      ).as("confidence")
    )
    sentimentDf.show(50, false)

    sentimentDf.groupBy("text_id", "label").agg(avg("confidence")).show(false)
  }
}
