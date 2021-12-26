package core

import models.{Feed, FeedURL, FeedContent, Report}
import play.api.Logger
import javax.inject._
import play.api.inject.ApplicationLifecycle

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import com.johnsnowlabs.nlp.SparkNLP
import com.johnsnowlabs.nlp.{DocumentAssembler, Finisher}
import com.johnsnowlabs.nlp.annotators.{StopWordsCleaner, Tokenizer}

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
  spark.sparkContext.setLogLevel("WARN")

  // https://nlp.johnsnowlabs.com/docs/en/pipelines#recognizeentitiesdl
  private val entityRecognitionPipeline =
    new PretrainedPipeline("onto_recognize_entities_sm", lang = "en")
  // new PretrainedPipeline("nerdl_fewnerd_subentity_100d_pipeline", lang = "en")
  // PretrainedPipeline("recognize_entities_dl", lang = "en")

  private val sentimetPipeline =
    new PretrainedPipeline("analyze_sentiment", lang = "en")

  private val marketSentimentPipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document"),
          BertSentenceEmbeddings
            .pretrained("sent_bert_wiki_books_sst2", "en")
            .setInputCols(Array("document"))
            .setOutputCol("sentence_embeddings"),
          ClassifierDLModel
            .pretrained("classifierdl_bertwiki_finance_sentiment", "en")
            .setInputCols(Array("document", "sentence_embeddings"))
            .setOutputCol("class")
        )
      )
      .fit(spark.createDataFrame(Seq()).toDF("text"))
  )

  private def getEntities(contentsDf: DataFrame): DataFrame = {

    val transformed = entityRecognitionPipeline.transform(contentsDf)
    log.info(s"Extracted entities from ${transformed.count()} blocks of text.")
    transformed.select("id", "entities").limit(3).show(false)
    // Every ROW contains the text with an array of entities and
    // corresponding array of maps that contain the tags for each entity.
    // to get all entities and their tags, need to explode each array column.
    // Which creates duplicate rows. Select col("text") when debugging.
    val entitiesDf = transformed
      .select(
        // expand entities array to individual rows
        col("id").as("textID"),
        explode(col("entities")).as("entity")
      )
      .select(
        // extract values from each entity struct
        col("textID"),
        col("entity.result").as("entityName"),
        col("entity.metadata.entity").as("entityType")
      )
    log.info(s"Extracted ${entitiesDf.count()} entities and their types.")
    entitiesDf.limit(7).show(false)
    entitiesDf
  }

  private def getSentiment(contentsDf: DataFrame): Unit = {
    val transformed =
      marketSentimentPipeline.transform(contentsDf)
    log.info(s"Extracted sentiment from ${transformed.count()} blocks of text.")
    transformed.printSchema()
    transformed.select("id", "text", "sentiment").limit(3).show(false)
  }

  def generateReport(contents: Seq[FeedContent]): Option[Report] = {
    import spark.implicits._

    log.info(s"Performing analysis for ${contents.size} articles.")
    log.info(s"Connected to spark version ${spark.version}")

    val allTitles = contents.zipWithIndex.map {
      case (c: FeedContent, idx: Int) =>
        (idx, c.title)
    }
    val allDescriptions = contents.zipWithIndex.map {
      case (c: FeedContent, idx: Int) =>
        (idx + contents.size + 1, c.body)
    }
    val contentsDf = spark
      .createDataFrame(
        allTitles ++ allDescriptions
      )
      .toDF("id", "text")

    // val entitiesDf = getEntities(contentsDf)
    val sentimentDf = getSentiment(contentsDf)
    None
  }

  lifecycle.addStopHook { () =>
    Future.successful {
      log.info("Analytics engine end hook called")
      // spark.stop()
    }
  }
}
