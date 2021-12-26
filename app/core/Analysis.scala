package core

import models.{Feed, FeedURL, FeedContent, AnalysisRow}
import play.api.Logger
import javax.inject._
import play.api.inject.ApplicationLifecycle

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.ml.feature.{RegexTokenizer}

import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import com.johnsnowlabs.nlp.SparkNLP
import com.johnsnowlabs.nlp.{DocumentAssembler, Finisher}
import com.johnsnowlabs.nlp.annotators.{StopWordsCleaner, Tokenizer, Normalizer}

import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.SparkSession

import scala.concurrent.Future
import scala.collection.mutable.WrappedArray
import scala.collection.JavaConverters._
import com.johnsnowlabs.nlp.embeddings.BertSentenceEmbeddings
import com.johnsnowlabs.nlp.annotators.classifier.dl.ClassifierDLModel
import com.johnsnowlabs.nlp.LightPipeline

@Singleton
class Analysis @Inject() (lifecycle: ApplicationLifecycle) {
  val log: Logger = Logger(this.getClass())

  private val spark: SparkSession =
    SparkSession
      .builder()
      .appName("RSS to Email")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .config("spark.kryoserializer.buffer.max", "100M")
      .config("spark.executor.memory", "500M")
      .master("local")
      .config("spark.driver.memory", "100M")
      .getOrCreate()
  import spark.implicits._

  // https://nlp.johnsnowlabs.com/docs/en/pipelines#recognizeentitiesdl
  private val entityRecognitionPipeline =
    new PretrainedPipeline("onto_recognize_entities_sm", lang = "en")
  // new PretrainedPipeline("nerdl_fewnerd_subentity_100d_pipeline", lang = "en")
  // PretrainedPipeline("recognize_entities_dl", lang = "en")

  private val sentimetPipeline =
    new PretrainedPipeline("analyze_sentiment", lang = "en")

  private val cleaningPipeline =
    // removes html tags from string and splits into words
    new RegexTokenizer()
      .setInputCol("rawText")
      .setOutputCol("normalized")
      .setPattern("<[^>]+>|\\s+") // split by html tag or space
      .setToLowercase(false)

  private def getEntities(contentsDf: DataFrame): DataFrame = {
    val transformed = entityRecognitionPipeline.transform(contentsDf)
    // transformed.printSchema()
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
      contents: Seq[FeedContent],
      shouldClean: Boolean = false
  ): DataFrame = {
    val allTitles = contents.zipWithIndex.map {
      case (c: FeedContent, idx: Int) =>
        (idx, c.title)
    }
    val allDescriptions = contents.zipWithIndex.map {
      case (c: FeedContent, idx: Int) =>
        (idx + contents.size + 1, c.body)
    }
    val rawContentsDf = spark
      .createDataFrame(
        allTitles ++ allDescriptions
      )
      .toDF("id", "rawText")
    if (shouldClean) {
      val cleanedContentsDf = cleaningPipeline.transform(rawContentsDf)
      log.info(s"Cleaned ${cleanedContentsDf.count()} blocks of text.")

      val contentsDf =
        cleanedContentsDf
          .select(
            col("id"),
            concat_ws(" ", col("normalized")).as("text")
          )
          .filter("text != ''")

      contentsDf.sample(0.25).show(false)
      contentsDf
    } else {
      rawContentsDf.select(col("id"), col("rawText").as("text"))
    }
  }

  def generateReport(contents: Seq[FeedContent]): Array[AnalysisRow] = {

    log.info(s"Performing analysis for ${contents.size} articles.")

    val contentsDf = constructContentsDf(contents, true)
    val entitiesDf = getEntities(contentsDf)
    val sentimentDf = getSentiment(contentsDf)
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
    expandedDf
      .sample(0.5)
      .show(false)

    val resultDf = expandedDf
      .groupBy("entityName", "entityType", "sentiment")
      .agg(
        collect_set("text").as("texts"),
        countDistinct("text")
          .as("numTexts"),
        (sum("confidence") * sum("sentimentBlockLength"))
          .as("aggregateConfidence")
      )
      .where("""
        entityType != 'CARDINAL' 
        and entityType != 'ORDINAL' 
        and aggregateConfidence > 0
      """)
      .orderBy(col("numTexts").desc)
      .na
      .drop("any")

    resultDf.show()

    resultDf
      .as[AnalysisRow]
      .collect()
    // .take(resultDf.count().toInt)
  }

  lifecycle.addStopHook { () =>
    Future.successful {
      log.info("Analytics engine end hook called")
      // spark.stop()
    }
  }
}
