package fyi.newssnips.datacruncher.core

import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import com.johnsnowlabs.nlp.LightPipeline

import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.DfUtils
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.SparkSession
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.nlp.DocumentAssembler
import org.apache.spark.sql.functions._
import com.johnsnowlabs.nlp.LightPipeline
import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline

@Singleton
class Analysis(spark: SparkSession) {
  private val log: Logger = Logger("app." + this.getClass().toString())
  import spark.implicits._

  // https://nlp.johnsnowlabs.com/docs/en/pipelines#recognizeentitiesdl
  // https://nlp.johnsnowlabs.com/demo

  log.info("Initalizing analysis pipelines.")
  log.info("Initalizing sentiment pipeline.")
  
  private lazy val sentimentPipeline =
    new LightPipeline(
      new Pipeline()
        .setStages(
          Array(
            new DocumentAssembler()
              .setInputCol("text")
              .setOutputCol("document"),
            new Tokenizer()
              .setInputCols("document")
              .setOutputCol("token"),
            BertForSequenceClassification
              .pretrained("bert_sequence_classifier_finbert", "en")
              .setInputCols("document", "token")
              .setOutputCol("sentiment")
              .setCaseSensitive(true)
              .setMaxSentenceLength(512)
          )
        )
        .fit(Seq[String]().toDF("text"))
    )

  log.info("Initalizing entity pipeline.")
  private lazy val entityRecognitionPipeline =
    new LightPipeline(
      new PretrainedPipeline(
        "onto_recognize_entities_electra_base",
        lang = "en"
      ).model
    )

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
    log.info(s"Extracted entities and their types.")
    DfUtils.showSample(entitiesDf)
    entitiesDf
  }

  private def getSentiment(contentsDf: DataFrame): DataFrame = {
    val transformed =
      sentimentPipeline.transform(contentsDf)
    log.info(s"Extracted sentiment from blocks of text.")
    transformed.printSchema()
    transformed.show(false)

    val sentimentDf = transformed
      .select(
        col("text_id"),
        explode(col("sentiment")).as("sentiment")
      )
      .select(
        col("text_id"),
        col("sentiment.result").as("sentiment"),
        element_at(col("sentiment.metadata"), col("sentiment.result"))
          .as("confidence"),
        (col("sentiment.end") - col("sentiment.begin"))
          .as("sentimentBlockLength")
      )
    log.info(s"Extracted sentiment blocks.")
    sentimentDf.show(false)
    DfUtils.showSample(sentimentDf, 5f)
    sys.exit(1)

    sentimentDf
  }

  def generateReport(contentsDf: DataFrame): Option[DataFrame] = {

    log.info(s"Performing analysis on ${contentsDf.count()} mentions.")

    val sentimentDf = getSentiment(contentsDf)
    val entitiesDf  = getEntities(contentsDf)

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
      s"Constructed expanded result of entities, sentiments " +
        "and their accompnying texts."
    )
    DfUtils.showSample(expandedDf)

    val negCondition = col("sentiment") === "negative"
    val posCondition = col("sentiment") === "positive"
    val typesToSkip  = Seq("CARDINAL", "ORDINAL", "PERCENT", "WORK_OF_ART")

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
      .filter(!col("entityType").isInCollection(typesToSkip))
      .where("""
        aggregateConfidence > 0
      """)
      .orderBy(col("totalNumTexts").desc)
      .na
      .drop("any")

    log.info(s"Analysis report constructed.")
    DfUtils.showSample(resultDf)
    Some(resultDf)
  }

  // lifecycle.addStopHook { () =>
  //   Future.successful {
  //     log.info("Analytics engine end hook called")
  //     // spark.stop()
  //   }
  // }
}
