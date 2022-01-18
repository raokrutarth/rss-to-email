package fyi.newssnips.datacruncher.scripts

import java.nio.file.Paths
import com.typesafe.scalalogging.Logger
import org.apache.spark.ml._
import org.apache.spark.sql.SparkSession
import configuration.AppConfig
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.nlp._
import java.nio.file.Path
import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline

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
  def storeNerPipeline(spark: SparkSession, model: String = "electra") = {
    import spark.implicits._
    log.info("Storing NER pipeline.")

    val pipeline = model match {

      case "electra" =>
        new PretrainedPipeline(
          "onto_recognize_entities_electra_base",
          lang = "en"
        ).model
      case "roberta" =>
        // Uses 4GB+ memory just for model
        new Pipeline()
          .setStages(
            Array(
              new DocumentAssembler()
                .setInputCol("text")
                .setOutputCol("document"),
              new Tokenizer()
                .setInputCols("document")
                .setOutputCol("token"),
              RoBertaForTokenClassification
                // roberta_base_token_classifier_conll03 OR roberta_base_token_classifier_ontonotes
                .pretrained("roberta_base_token_classifier_conll03", "en")
                .setInputCols("document", "token")
                .setOutputCol("ner")
                .setCaseSensitive(true)
                .setMaxSentenceLength(512),
              // since output column is IOB/IOB2 style, NerConverter can extract entities
              new NerConverter()
                .setInputCols("document", "token", "ner")
                .setOutputCol("entities")
            )
          )
          .fit(Seq[String]().toDF("text"))
    }

    savePipeline(
      pipeline,
      nerModelPath
    )
  }

  def storeSentimentPipeline(
      spark: SparkSession,
      model: String = "distilbert"
  ) = {
    import spark.implicits._

    val pipeline = model match {
      case "distilbert" => {
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
          .fit(Seq[String]().toDF("text"))
      }
      case "roberta" => {
        log.info("Using roberta model for sentiment.")
        // 0	negative
        // 1	neutral
        // 2	positive
        new Pipeline()
          .setStages(
            Array(
              new DocumentAssembler()
                .setInputCol("text")
                .setOutputCol("document"),
              new Tokenizer()
                .setInputCols("document")
                .setOutputCol("token"),
              RoBertaForSequenceClassification
                // .pretrained("roberta_base_sequence_classifier_imdb", "en")
                .loadSavedModel(
                  "models/siebert/sentiment-roberta-large-english/saved_model/1",
                  Init.spark
                )
                .setInputCols("document", "token")
                .setOutputCol("sentiment")
                .setCaseSensitive(true)
                .setMaxSentenceLength(512)
            )
          )
          .fit(Seq[String]().toDF("text"))
      }
    }
    savePipeline(
      pipeline,
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
    if (ner) storeNerPipeline(spark)
    if (sentiment) storeSentimentPipeline(spark)

    spark.stop()
  }
}
