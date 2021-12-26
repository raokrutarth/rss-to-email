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
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.annotators.ner.NerConverter
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.util.Benchmark
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.SparkSession
import scala.concurrent.Future

@Singleton
class Analysis @Inject() (lifecycle: ApplicationLifecycle) {
  val log: Logger = Logger(this.getClass())
  private val spark: SparkSession =
    SparkSession
      .builder()
      .appName("RSS to Email")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .master("local")
      .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")
  import spark.implicits._

  val document = new DocumentAssembler()
    .setInputCol("text")
    .setOutputCol("document")

  val token = new Tokenizer()
    .setInputCols("document")
    .setOutputCol("token")

  val normalizer = new Normalizer()
    .setInputCols("token")
    .setOutputCol("normal")

  val wordEmbeddings = WordEmbeddingsModel
    .pretrained()
    .setInputCols("document", "token")
    .setOutputCol("word_embeddings")

  val ner = NerDLModel
    .pretrained()
    .setInputCols("normal", "document", "word_embeddings")
    .setOutputCol("ner")

  val nerConverter = new NerConverter()
    .setInputCols("document", "normal", "ner")
    .setOutputCol("ner_converter")

  val finisher = new Finisher()
    .setInputCols("ner", "ner_converter")
    .setIncludeMetadata(true)
    .setOutputAsArray(false)
    .setCleanAnnotations(false)
    .setAnnotationSplitSymbol("@")
    .setValueSplitSymbol("#")

  val pipeline = new Pipeline().setStages(
    Array(
      document,
      token,
      normalizer,
      wordEmbeddings,
      ner,
      nerConverter,
      finisher
    )
  )
  // TODO
  // - make connections between sources.
  // - connections between nouns/entities
  // - convert feeds to vectors using commom words
  // and determine similarity
  // - find repeating topics.
  // - top mentined entities.

  def generateReport(contents: Seq[FeedContent]): Option[Report] = {
    log.info(s"Performing analysis for content ${contents.size}")
    log.info(s"Connected to spark version ${spark.version}")

    val testing = Seq(
      (1, "Google is a famous company"),
      (2, "Peter Parker is a super heroe")
    ).toDS.toDF("_id", "text")

    val result =
      pipeline.fit(Seq.empty[String].toDS.toDF("text")).transform(testing)
    Benchmark.time("Time to convert and show") {
      result.select("ner", "ner_converter").show(truncate = false)
    }
    None
  }

  lifecycle.addStopHook { () =>
    Future.successful {
      log.info("Analytics engine end hook called")
      spark.stop()
    }
  }
}
