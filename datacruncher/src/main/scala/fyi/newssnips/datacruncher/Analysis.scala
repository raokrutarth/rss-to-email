package fyi.newssnips.datacruncher.core

import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import com.johnsnowlabs.nlp.LightPipeline

import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.DfUtils

@Singleton
class Analysis(spark: SparkSession) {
  private val log: Logger = Logger(this.getClass())
  import spark.implicits._

  // https://nlp.johnsnowlabs.com/docs/en/pipelines#recognizeentitiesdl
  // https://nlp.johnsnowlabs.com/demo

  log.info("Initalizing sentiment pipeline.")
  private val sentimetPipeline =
    new LightPipeline(
      new PretrainedPipeline(
        "analyze_sentiment",
        lang = "en"
      ).model
    )

  log.info("Initalizing entity pipeline.")
  private val entityRecognitionPipeline =
    new LightPipeline(
      new PretrainedPipeline(
        "onto_recognize_entities_electra_small",
        lang = "en"
      ).model
    )

  log.info("Analysis pipelines initalized.")

  private def getEntities(contentsDf: DataFrame): DataFrame = {
    val transformed = entityRecognitionPipeline.transform(contentsDf)
    log.info(s"Extracted entities from ${transformed.count()} blocks of text.")
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
        col("entity.result").as("entityName"),
        col("entity.metadata.entity").as("entityType")
      )
    log.info(s"Extracted ${entitiesDf.count()} entities and their types.")
    DfUtils.showSample(entitiesDf, 5f)
    entitiesDf
  }

  private def getSentiment(contentsDf: DataFrame): DataFrame = {
    val transformed =
      sentimetPipeline.transform(contentsDf)
    log.info(s"Extracted sentiment from ${transformed.count()} blocks of text.")

    // transformed.printSchema()

    val sentimentDf = transformed
      .select(
        col("text_id"),
        explode(col("sentiment")).as("sentiment")
      )
      .select(
        col("text_id"),
        col("sentiment.result").as("sentiment"),
        col("sentiment.metadata.confidence").as("confidence"),
        (col("sentiment.end") - col("sentiment.begin"))
          .as("sentimentBlockLength")
      )

    log.info(s"Extracted ${sentimentDf.count()} sentiment blocks.")
    DfUtils.showSample(sentimentDf, 5f)

    sentimentDf
  }

  def generateReport(contentsDf: DataFrame): Option[DataFrame] = {

    log.info(s"Performing analysis on ${contentsDf.count()} mentions.")

    val entitiesDf  = getEntities(contentsDf)
    val sentimentDf = getSentiment(contentsDf)

    val expandedDf = entitiesDf
      .join(
        sentimentDf,
        Seq("text_id")
      )
      .alias("entities_and_sentiments_df")
      .join(
        contentsDf,
        Seq("text_id")
      )
      .select(
        col("entities_and_sentiments_df.text_id"),
        col("entityName"),
        col("entityType"),
        col("sentiment"),
        col("confidence"),
        col("sentimentBlockLength"),
        col("text")
      )

    log.info(
      s"Constructed expanded result of ${expandedDf.count()} entities, sentiments " +
        "and their accompnying texts."
    )
    DfUtils.showSample(expandedDf, 5f)

    val negCondition = col("sentiment") === "negative"
    val posCondition = col("sentiment") === "positive"

    val resultDf = expandedDf
      .groupBy("entityName", "entityType")
      .agg(
        // get sentiment counts
        sum(when(negCondition, 1).otherwise(0)).as("negativeMentions"),
        sum(when(posCondition, 1).otherwise(0)).as("positiveMentions"),
        // collect relevant texts
        collect_set(when(posCondition, $"text_id")).as("positiveTextIds"),
        collect_set(when(negCondition, $"text_id")).as("negativeTextIds"),
        // count total mentions
        countDistinct("text_id").as("totalNumTexts"),
        // get approximate confidence of sentiment labeling.
        (sum("confidence") * sum("sentimentBlockLength"))
          .as("aggregateConfidence")
      )
      .where("""
        entityType != 'CARDINAL' 
        and entityType != 'ORDINAL' 
        and entityType != 'PERCENT'
        and aggregateConfidence > 0
      """)
      .orderBy(col("totalNumTexts").desc)
      .na
      .drop("any")

    log.info(s"Analysis resulted in ${resultDf.count()} entities of interest.")
    DfUtils.showSample(resultDf, 5f)
    Some(resultDf)
  }

  // lifecycle.addStopHook { () =>
  //   Future.successful {
  //     log.info("Analytics engine end hook called")
  //     // spark.stop()
  //   }
  // }
}
