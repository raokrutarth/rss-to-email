package fyi.newssnips.datacruncher.core

import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

import com.typesafe.scalalogging.Logger
import fyi.newssnips.datacruncher.utils.DfUtils
import com.johnsnowlabs.nlp.base._
import org.apache.spark.sql.SparkSession
import com.johnsnowlabs.nlp.base._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import com.johnsnowlabs.nlp.base._
import org.apache.spark.sql.SparkSession
import com.johnsnowlabs.nlp.base._
import org.apache.spark.sql.functions._
import com.johnsnowlabs.nlp.LightPipeline
import org.apache.spark.ml.PipelineModel
import fyi.newssnips.datacruncher.scripts.ModelStore
import fyi.newssnips.datacruncher._

@Singleton
class Analysis(spark: SparkSession) {
  private val log: Logger = Logger("app." + this.getClass().toString())
  import spark.implicits._

  // https://nlp.johnsnowlabs.com/docs/en/pipelines#recognizeentitiesdl
  // https://nlp.johnsnowlabs.com/demo

  private val sentimentPipelineSocial = {
    log.info("Initalizing sentiment pipeline.")
    PipelineModel.read.load(ModelStore.sentimentModelPath.toString())
  }

  private val entityRecognitionPipeline = {
    log.info("Initalizing NER pipeline.")

    new LightPipeline(
      PipelineModel.read.load(ModelStore.nerModelPath.toString())
    )
  }

  // TODO use it to remove stop words from entity names
  // private val stopWordsPipeline = new StopWordsRemover()
  // .setInputCol("raw")
  // .setOutputCol("filtered")

  log.info("Analysis pipelines initalized.")

  private def getEntities(contentsDf: DataFrame): DataFrame = {
    val transformed = entityRecognitionPipeline.transform(contentsDf)
    log.info(s"Extracted entities from blocks of text.")

    // Every ROW contains the text with an array of entities and
    // corresponding array of maps that contain the tags for each entity.
    // to get all entities and their tags, need to explode each array column.
    // Which creates duplicate rows. Select col("text") when debugging.
    val entitiesDf = transformed
      .select(
        // expand entities array to individual rows
        col("text_id"),
        explode(col("entities")).as("entity")
      )
      .select(
        // extract values from each entity struct
        col("text_id"),
        // remove starting and trailing punctuation from entity name
        // TODO use [^\x20-\x7E]+ to remove non-printable chars.
        regexp_replace(col("entity.result"), "(\\W+$|^\\W+)", "")
          .as("entityName"),
        col("entity.metadata.entity").as("entityType")
      )
      .filter(!col("entityType").isInCollection(ManualOverrides.typesToSkip))
      .filter(
        !lower(col("entityName")).isInCollection(ManualOverrides.entitiesToSkip)
      )
      .withColumn("entityName", initcap(col("entityName"))) // normalize case
      .withColumn(
        "entityName",
        // manual fixes
        NerHelper.entityNameNormalizeUdf(col("entityName"))
      )

    log.info(s"Constructed entity extraction query.")
    DfUtils.showSample(entitiesDf)
    entitiesDf
  }

  private def getSentiment(
      contentsDf: DataFrame
  ): DataFrame = {
    val transformed =
      sentimentPipelineSocial.transform(contentsDf)
    log.info(s"Extracting sentiment from blocks of text.")

    val negOverrides    = ManualOverrides.negativePhrases
    val overrideTextcol = lower(col("text"))

    val sentimentDf = transformed
      .withColumn(
        "sentiment",
        explode(col("sentiment")).as("sentiment")
      )
      .filter(
        (col("sentiment.end") - col("sentiment.begin")) > 4
      ) // remove junk/trivial text blocks
      .withColumn(
        "confidence",
        array_max(
          map_values($"sentiment.metadata").cast(ArrayType(DoubleType))
        )
      )
      .withColumn(
        "sentiment",
        col("sentiment.result")
      )
      .withColumn(
        "sentiment",
        expr(
          """ 
            CASE 
              WHEN lower(sentiment) like 'p%' THEN 'pos'
              WHEN lower(sentiment) like 'n%' THEN 'neg'
              ELSE lower(sentiment) 
            END
          """
        )
      )
      .withColumn(
        "sentiment",
        when(
          // manual overrides for texts with known negative sentiment.
          negOverrides.map(overrideTextcol.contains).reduce(_ || _),
          "neg"
        )
          .otherwise(col("sentiment"))
      )
      .select(
        col("text_id"),
        col("sentiment")
      )

    log.info(s"Extracted sentiment blocks.")
    DfUtils.showSample(sentimentDf, truncate = 300)
    sentimentDf
  }

  def generateReport(
      contentsDf: DataFrame
  ): Option[DataFrame] = {

    log.info(s"Performing analysis on ${contentsDf.count()} mentions.")

    val sentimentDf = getSentiment(contentsDf)
    val entitiesDf  = getEntities(contentsDf)

    val expandedDf = entitiesDf
      .join(
        sentimentDf,
        Seq("text_id")
      )
      // inner join with contents DF here to get raw text
      .select(
        entitiesDf("text_id"),
        col("entityName"),
        col("entityType"),
        col("sentiment")
        // col("confidence")
      )

    log.info(
      s"Constructed intermediate result of entities & sentiments."
    )

    val resultDf = expandedDf
      .groupBy("entityName")
      .agg(
        // collect relevant texts
        collect_set(when(col("sentiment") === "pos", $"text_id"))
          .as("positiveTextIds"),
        collect_set(when(col("sentiment") === "neg", $"text_id"))
          .as("negativeTextIds"),
        // count total mentions
        countDistinct("text_id").as("totalNumTexts"),
        first("entityType").as("entityType")
      )
      // get sentiment counts
      .withColumn("negativeMentions", size(col("negativeTextIds")))
      .withColumn("positiveMentions", size(col("positiveTextIds")))
      .na
      .drop("any")

    log.info(s"Analysis report query constructed.")

    DfUtils.showSample(resultDf)
    Some(resultDf)
  }
}
