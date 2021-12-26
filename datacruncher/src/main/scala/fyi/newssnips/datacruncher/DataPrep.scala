package fyi.newssnips.datacruncher

import fyi.newssnips.models.FeedContent
import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.RegexTokenizer
import org.apache.spark.ml.Pipeline

import com.typesafe.scalalogging.Logger
import fyi.newssnips.datacruncher.utils.DfUtils
import fyi.newssnips.datacruncher.scripts.ModelStore
import org.apache.spark.ml.PipelineModel

@Singleton
class DataPrep(spark: SparkSession) {
  import spark.implicits._
  private val log: Logger = Logger("app." + this.getClass().toString())

  private val cleaningPipeline = new Pipeline()
    .setStages(
      Array(
        // removes html tags from string and splits into words
        new RegexTokenizer()
          .setInputCol("rawText")
          .setOutputCol("normalized")
          .setPattern("<[^>]+>|\\s+") // split by html tag or space
          .setToLowercase(false)
      )
    )
    .fit(Seq[String]().toDF("rawText"))

  private val sentencePipeline =
    PipelineModel.read.load(ModelStore.cleanupPipelinePath.toString())

  def constructContentsDf(
      contents: Seq[FeedContent]
  ): DataFrame = {
    // Break down descriptions into clean sentences
    val descriptionsDf = spark.sparkContext
      .parallelize(contents.map { c => (c.url, c.body) })
      .toDF("url", "rawText")
    log.info(s"Identified blocks of description.")

    val cleanedDescriptionsDf = cleaningPipeline
      .transform(descriptionsDf)
      .dropDuplicates("url")
      .select(
        concat_ws(" ", col("normalized")).as("textBlock"),
        col("url")
      )

    val descriptionSentencesDf = sentencePipeline
      .transform(cleanedDescriptionsDf)
      .select(
        // expand array of sentences to individual rows
        explode(col("sentence.result")).as("text"),
        col("url")
      )
    log.info(
      s"Cleaned and extracted sentences from descriptions."
    )

    // prepare titles
    val titlesDf = spark.sparkContext
      .parallelize(contents.map { c => (c.url, c.title) })
      .toDF("url", "rawText")

    val cleanTitlesDf = cleaningPipeline
      .transform(titlesDf)
      .dropDuplicates("url")
      .select(
        concat_ws(" ", col("normalized")).as("text"),
        col("url")
      )
    log.info(
      s"Cleaned and extracted titles."
    )

    val contentsDf =
      cleanTitlesDf
        .union(descriptionSentencesDf)
        // remove html escape tags.
        .withColumn("text", regexp_replace(col("text"), "\\s*&\\S*;\\s*", " "))
        // remove URLs
        .withColumn(
          "text",
          regexp_replace(
            col("text"),
            "\\bhttps?://\\S+\\b",
            " "
          )
        )
        .select(
          monotonically_increasing_id().as("id"),
          // replace html escape tags and trim whitespace.
          // TODO escape URLs
          trim(col("text")).as("text"),
          col("url")
        )
        .filter("text != ''")

    log.info(
      s"Extracted sentences from titles and descriptions."
    )
    DfUtils.showSample(contentsDf)
    contentsDf
  }

  /* given a contents DF with each sentence pegged to a URL, seperate the
   * dataframe with duplicated values to two seperate dataframes. */
  def seperateLinksFromContents(
      contentsDf: DataFrame
  ): (DataFrame, DataFrame) = {
    log.info(s"Seperating URLs from thick contents dataframe.")

    val urlDf = contentsDf
      .select($"url")
      .distinct()
      .select(
        monotonically_increasing_id().as("link_id"),
        col("url")
      )

    log.info(s"Extracted unique URLs.")
    DfUtils.showSample(df = urlDf)

    val slimContentsDf = contentsDf
      .alias("cnt_df")
      .join(
        urlDf.alias("url_df"),
        col("cnt_df.url") === col("url_df.url")
      )
      .select(
        col("cnt_df.id").as("text_id"),
        col("url_df.link_id"),
        col("cnt_df.text")
      )
    log.info("Constructed slim contents dataframe.")
    DfUtils.showSample(slimContentsDf)

    (urlDf, slimContentsDf)
  }
}
