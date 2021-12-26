package fyi.newssnips.datacruncher

import fyi.newssnips.models.FeedContent
import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.RegexTokenizer
import org.apache.spark.ml.Pipeline

import com.johnsnowlabs.nlp.DocumentAssembler

import com.johnsnowlabs.nlp.annotators.sentence_detector_dl.SentenceDetectorDLModel
import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.DfUtils

@Singleton
class DataPrep(spark: SparkSession) {
  import spark.implicits._
  private val log: Logger = Logger("app." + this.getClass().toString())

  private val cleaningPipeline = new Pipeline().setStages(
    Array(
      // removes html tags from string and splits into words
      new RegexTokenizer()
        .setInputCol("rawText")
        .setOutputCol("normalized")
        .setPattern("<[^>]+>|\\s+") // split by html tag or space
        .setToLowercase(false)
    )
  )

  private val sentencePipeline = new Pipeline().setStages(
    Array(
      new DocumentAssembler()
        .setInputCol("textBlock")
        .setOutputCol("document"),
      SentenceDetectorDLModel
        .pretrained("sentence_detector_dl", "en")
        .setInputCols(Array("document"))
        .setOutputCol("sentence")
    )
  )

  def constructContentsDf(
      contents: Seq[FeedContent]
  ): DataFrame = {
    // Break down descriptions into clean sentences
    val descriptionsDf = spark.sparkContext
      .parallelize(contents.map { c => (c.url, c.body) })
      .toDF("url", "rawText")
    log.info(s"Identified blocks of description.")

    val cleanedDescriptionsDf = cleaningPipeline
      .fit(descriptionsDf)
      .transform(descriptionsDf)
      .dropDuplicates("url")
      .select(
        concat_ws(" ", col("normalized")).as("textBlock"),
        col("url")
      )
    log.debug("Cleaned descriptions.")

    val descriptionSentencesDf = sentencePipeline
      .fit(cleanedDescriptionsDf)
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
      .fit(titlesDf)
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
        .select(
          monotonically_increasing_id().as("id"),

          // replace html escape tags and trim whitespace.
          // TODO escape URLs
          trim(regexp_replace(col("text"), "&#\\S*", "")).as("text"),
          col("url")
        )
        .filter("text != ''")
        .dropDuplicates("text")

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
    DfUtils.showSample(df = urlDf, truncate = false)

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
