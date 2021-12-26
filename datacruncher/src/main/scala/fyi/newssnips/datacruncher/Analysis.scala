package fyi.newssnips.datacruncher.core

import fyi.newssnips.models.{AnalysisRow, Feed, FeedContent, FeedURL}
import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.ml.feature.RegexTokenizer
import org.apache.spark.ml.Pipeline

import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import com.johnsnowlabs.nlp.SparkNLP
import com.johnsnowlabs.nlp.{DocumentAssembler, Finisher}
import com.johnsnowlabs.nlp.annotators.{Normalizer, StopWordsCleaner, Tokenizer}

import scala.concurrent.Future
import scala.collection.mutable.WrappedArray
import scala.collection.JavaConverters._
import com.johnsnowlabs.nlp.embeddings.BertSentenceEmbeddings
import com.johnsnowlabs.nlp.annotators.classifier.dl.ClassifierDLModel
import com.johnsnowlabs.nlp.LightPipeline
import com.johnsnowlabs.nlp.annotators.sentence_detector_dl.SentenceDetectorDLModel
import com.typesafe.scalalogging.Logger

@Singleton
class Analysis() {
  val log: Logger = Logger(this.getClass())

  private val spark: SparkSession =
    SparkSession
      .builder()
      .appName("RSS to Email")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      // .config("spark.kryoserializer.buffer.max", "100M")
      .master("local[*]")
      // .config("spark.driver.memory", "100M")
      .getOrCreate()
  import spark.implicits._

  // https://nlp.johnsnowlabs.com/docs/en/pipelines#recognizeentitiesdl
  private val entityRecognitionPipeline =
    new PretrainedPipeline("onto_recognize_entities_sm", lang = "en")

  private val sentimetPipeline =
    new PretrainedPipeline("analyze_sentiment", lang = "en")

  private val cleaningPipeline =
    // removes html tags from string and splits into words
    new RegexTokenizer()
      .setInputCol("rawText")
      .setOutputCol("normalized")
      .setPattern("<[^>]+>|\\s+") // split by html tag or space
      .setToLowercase(false)

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
        col("id").as("textId"),
        explode(col("entities")).as("entity")
      )
      .select(
        // extract values from each entity struct
        col("textId"),
        col("entity.result").as("entityName"),
        col("entity.metadata.entity").as("entityType")
      )
    log.info(s"Extracted ${entitiesDf.count()} entities and their types.")
    entitiesDf.limit(7).show(false)
    entitiesDf
  }

  private def getSentiment(contentsDf: DataFrame): DataFrame = {
    val transformed =
      sentimetPipeline.transform(contentsDf)
    log.info(s"Extracted sentiment from ${transformed.count()} blocks of text.")

    // transformed.printSchema()

    val sentimentDf = transformed
      .select(
        col("id").as("textId"),
        explode(col("sentiment")).as("sentiment")
      )
      .select(
        col("textId"),
        col("sentiment.result").as("sentiment"),
        col("sentiment.metadata.confidence").as("confidence"),
        (col("sentiment.end") - col("sentiment.begin"))
          .as("sentimentBlockLength")
      )

    log.info(s"Extracted ${sentimentDf.count()} sentiment blocks.")
    sentimentDf
      .limit(10)
      .show(false)

    sentimentDf
  }

  private def constructContentsDf(
      contents: Seq[FeedContent]
  ): DataFrame = {
    // Break down descriptions into clean sentences
    val descriptionsDf = spark.sparkContext
      .parallelize(contents.map { c => c.body })
      .toDF("rawText")
    log.info(s"Identified ${descriptionsDf.count()} blocks of description.")

    val cleanedDescriptionsDf = cleaningPipeline
      .transform(descriptionsDf)
      .select(
        concat_ws(" ", col("normalized")).as("textBlock")
      )

    val descriptionSentencesDf = sentencePipeline
      .fit(cleanedDescriptionsDf)
      .transform(cleanedDescriptionsDf)
      .select(
        // expand array of sentences to individual rows
        explode(col("sentence.result")).as("text")
      )

    log.info(
      s"Cleaned and extracted ${descriptionSentencesDf.count()} sentences from descriptions."
    )

    // prepare titles
    val cleanTitlesDf = cleaningPipeline
      .transform(
        spark.sparkContext
          .parallelize(contents.map { c => c.title })
          .toDF("rawText")
      )
      .select(
        concat_ws(" ", col("normalized")).as("text")
      )
    log.info(
      s"Cleaned and extracted ${cleanTitlesDf.count()} titles."
    )

    val contentsDf =
      cleanTitlesDf
        .union(descriptionSentencesDf)
        .select(monotonically_increasing_id().as("id"), col("text"))
        .filter("text != ''")

    log.info(
      s"Extracted a total of ${contentsDf.count()} sentences from titles and descriptions."
    )
    contentsDf.sample(5f / math.max(5, contentsDf.count())).show(false)
    contentsDf
  }

  def generateReport(contents: Seq[FeedContent]): Array[AnalysisRow] = {

    log.info(s"Performing analysis for ${contents.size} articles.")

    /* TODO
     * - get counts per (entity, ET, sentiment)
     * - get rows of (E, ET, neg-sentences, pos-sentences, n-count, p-count) */
    val contentsDf = constructContentsDf(contents)
    contentsDf.cache()
    val entitiesDf  = getEntities(contentsDf)
    val sentimentDf = getSentiment(contentsDf)

    entitiesDf.cache()
    sentimentDf.cache()
    val expandedDf = entitiesDf
      .join(
        sentimentDf,
        sentimentDf("textId") === entitiesDf("textId"),
        "inner"
      )
      // not the ideal join given the
      .join(contentsDf, contentsDf("id") === sentimentDf("textId"))
      .select(
        sentimentDf("textId"),
        col("entityName"),
        col("entityType"),
        col("sentiment"),
        col("confidence"),
        col("sentimentBlockLength"),
        col("text")
      )
    log.info(
      s"Constructed expanded result of ${expandedDf.count()} entities and their accompnying texts."
    )
    expandedDf
      .sample(5f / math.max(5, expandedDf.count()))
      .show()

    contentsDf.unpersist()
    entitiesDf.unpersist()
    sentimentDf.unpersist()

    val negCondition = col("sentiment") === "negative"
    val posCondition = col("sentiment") === "positive"

    val resultDf = expandedDf
      .groupBy("entityName", "entityType")
      .agg(
        // get sentiment counts
        sum(when(negCondition, 1).otherwise(0)).as("negativeMentions"),
        sum(when(posCondition, 1).otherwise(0)).as("positiveMentions"),
        // collect relevant texts
        collect_set(when(posCondition, $"textId")).as("positiveTextIds"),
        collect_set(when(negCondition, $"textId")).as("negativeTextIds"),
        // count total mentions
        countDistinct("textId").as("totalNumTexts"),
        // get approximate confidence of sentiment labeling.
        (sum("confidence") * sum("sentimentBlockLength"))
          .as("aggregateConfidence")
      )
      .where("""
        entityType != 'CARDINAL' 
        and entityType != 'ORDINAL' 
        and aggregateConfidence > 0
      """)
      .orderBy(col("totalNumTexts").desc)
      .na
      .drop("any")

    log.info(s"Analysis resulted in ${resultDf.count()} results")
    resultDf.sample(5f / math.max(5, resultDf.count())).show()
    resultDf
      .as[AnalysisRow]
      .collect()

  }

  def cleanup() = spark.stop()

  // lifecycle.addStopHook { () =>
  //   Future.successful {
  //     log.info("Analytics engine end hook called")
  //     // spark.stop()
  //   }
  // }
}
