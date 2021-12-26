package fyi.newssnips.datacruncher.scripts

import java.nio.file.Paths
import com.typesafe.scalalogging.Logger
import org.apache.spark.ml._
import org.apache.spark.sql.SparkSession
import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import configuration.AppConfig
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.nlp._
import java.nio.file.Path

/** One-time utility created to store the needed
  *
  * Only directories with names ending with _pipeline are copied to the
  * dockerimage.
  */

object ModelStore {
  private val log       = Logger("app." + this.getClass().toString())
  private val modelsDir = Paths.get(AppConfig.settings.modelsPath)

  val cleanupPipelinePath = modelsDir.resolve("cleanup_pipeline")
  val nerModelPath        = modelsDir.resolve("ner_pipeline")
  val sentimentModelPath  = modelsDir.resolve("sentiment_pipeline")

  private def savePipeline(pipeline: PipelineModel, pipelinePath: Path) = {
    log.info(s"Saving pipeline to ${pipelinePath.toString()}.")

    pipeline.write.overwrite.save(pipelinePath.toString())

    // verify read
    val p = PipelineModel.read.load(pipelinePath.toString())
    log.info(
      s"Saved pipeline ${p.uid} and confirmed read from ${pipelinePath.toString()}."
    )
  }

  def storeSentencePipeline(spark: SparkSession) = {
    import spark.implicits._

    savePipeline(
      new Pipeline()
        .setStages(
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
        .fit(Seq[String]().toDF("textBlock")),
      cleanupPipelinePath
    )

  }
  def storeNerPipeline() {
    log.info("Storing NER pipeline.")
    savePipeline(
      new PretrainedPipeline(
        "onto_recognize_entities_electra_base",
        lang = "en"
      ).model,
      nerModelPath
    )
  }

  def storeSentimentPipeline(spark: SparkSession) = {
    import spark.implicits._

    savePipeline(
      new Pipeline()
        .setStages(
          Array(
            new DocumentAssembler()
              .setInputCol("text")
              .setOutputCol("document"),
            new Tokenizer()
              .setInputCols("document")
              .setOutputCol("token"),
            DistilBertForSequenceClassification
              .loadSavedModel(
                "models/distilbert-base-uncased-finetuned-sst-2-english/saved_model/1",
                Init.spark
              )
              .setInputCols(Array("document", "token"))
              .setOutputCol("sentiment")
              .setCaseSensitive(false)
              .setMaxSentenceLength(512)
              .setCoalesceSentences(true)
          )
        )
        .fit(Seq[String]().toDF("text")),
      sentimentModelPath
    )
  }

  def storePipelines(
      ner: Boolean = true,
      sentence: Boolean = true,
      sentiment: Boolean = true
  ) = {
    log.info(s"Saving models to ${modelsDir.toString()}")

    lazy val spark: SparkSession =
      SparkSession
        .builder()
        .appName("newssnips.fyi")
        .config(
          "spark.serializer",
          "org.apache.spark.serializer.KryoSerializer"
        )
        .master("local[*]")
        .getOrCreate()

    if (sentence) storeSentencePipeline(spark)
    if (ner) storeNerPipeline()
    if (sentiment) storeSentimentPipeline(spark)

    spark.stop()
  }
}
