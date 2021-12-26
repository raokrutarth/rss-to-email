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

import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.SparkSession

import scala.concurrent.Future
import scala.collection.mutable.WrappedArray
import scala.collection.JavaConverters._

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

  private def getEntities(contentsDf: DataFrame): Map[String, String] = {

    val transformed = entityRecognitionPipeline.transform(contentsDf)
    log.info(s"Extracted entities from ${transformed.count()} blocks of text.")
    transformed.select("id", "text", "entities").limit(3).show(false)
    // Every ROW contains the text with an array of entities and
    // corresponding array of maps that contain the tags for each entity.
    // to get all entities and their tags, need to explode each array column.
    // Which creates duplicate rows. Select col("text") when debugging.
    val selected = transformed
      .select(
        // expand entities to individual rows
        col("id").as("textID"),
        col("entities.metadata").as("metadata"),
        explode(col("entities.result")).as("extractedEntity")
      )
      .select(
        // expand tags to individual rows
        col("textID"),
        col("extractedEntity"),
        explode(col("metadata")).as("extractedTags")
      )
      // extract tags
      .select(
        col("textID"),
        col("extractedEntity"),
        col("extractedTags.entity").as("entityType")
      )
    log.info(s"Extracted ${selected.count()} entities and their tags.")
    selected.limit(7).show(false)

    val entitiesToMetadatas = selected
      .collectAsList()
      .asScala
      .toList
    // .collect
    // .map(_.toSeq)
    // .flatten
    // .toList

    log.info(
      s"first: ${entitiesToMetadatas.head}, second: ${entitiesToMetadatas(1)}"
    )

    // entitiesToMetadatas.map {
    //   (
    //       entities: WrappedArray[String],
    //       metadatas: WrappedArray[Map[String, String]]
    //   ) =>
    //     (entities, metadatas).zipped.foreach { (e, m) => (e, m("entity")) }
    // }
    // log.info(s"entities to metadata tuple $entitiesToMetadatas")

    // val result = entitiesToMetadatas.flatMap { case (entities, metadatas) =>
    //   for (
    //     e <- entities;
    //     m <- metadatas
    //   ) yield (e, m("entity"))
    // }.toMap
    // log.info(s"results: ${result}")
    // result
    Map()
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

    val entities = getEntities(contentsDf)
    None
  }

  lifecycle.addStopHook { () =>
    Future.successful {
      log.info("Analytics engine end hook called")
      // spark.stop()
    }
  }
}
