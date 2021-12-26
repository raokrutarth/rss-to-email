package fyi.newssnips.datacruncher

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

class data(spark: SparkSession) {
  import spark.implicits._
  val example = Seq(
    "Wyoming GOP votes to no longer recognize Rep. Liz Cheney as a Republican.",
    "Aung San Suu Kyi being treated well: Myanmar army (BBC).",
    "Jeremy was positive but the federal reserver thought people are wrong.",
    "Plus, Chris talks about the battle for the living room with NYU Professor Scott Galloway, author of The Four: The Hidden DNA of Amazon, Apple, Facebook, and Google.",
    "Google has its best week in more than ten years.",
    "Is Amazon's new venture with Google a serious threat to Apple?"
  ).toDS.toDF("text")
}

/*
    sentiment scores:
        "bert_base_sequence_classifier_imdb" 6/6
        "bert_sequence_classifier_finbert" 4/6 (neutral too often)
        sent_bert_wiki_books_sst2 (neutral more often. wrong. can try with other embeddings.)
        bert_large_sequence_classifier_imdb ( 6 gb memory usage)
        analyze_sentimentdl_use_twitter (5/6 6gb memory usage)

 */

object Sentiment1 {
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("newssnips.fyi")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .master("local[*]")
      .getOrCreate()

//   val document_assembler = new DocumentAssembler()
//     .setInputCol("text")
//     .setOutputCol("document")

//   val tokenizer = new Tokenizer()
//     .setInputCols("document")
//     .setOutputCol("token")

//   val tokenClassifier =
//     BertForSequenceClassification
//       .pretrained("bert_large_sequence_classifier_imdb", "en") //
//       .setInputCols("document", "token")
//       .setOutputCol("class")
//       .setCaseSensitive(true)
//       .setMaxSentenceLength(512)

  val pipeline =
    new PretrainedPipeline("analyze_sentimentdl_use_twitter", lang = "en")

//       new Pipeline().setStages(
//     Array(document_assembler, tokenizer, tokenClassifier)
//   )

  val input = new data(spark).example
//   val result = pipeline.fit(input).transform(input)
  val result = pipeline.transform(input)
  result.printSchema()
  result.show(false)
  println("\n\n\n\n")
  result.select(col("text"), explode(col("sentiment"))).show(false)
  spark.stop()
}

object Sentiment2 {
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("newssnips.fyi")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .master("local[*]")
      .getOrCreate()

  val document = new DocumentAssembler()
    .setInputCol("text")
    .setOutputCol("document")

  import spark.implicits._

  val embeddings = BertSentenceEmbeddings
    .pretrained("sent_bert_wiki_books_sst2", "en")
    .setInputCols(Array("document"))
    .setOutputCol("sentence_embeddings")

  val sentimentClassifier = ClassifierDLModel
    .pretrained("classifierdl_bertwiki_finance_sentiment", "en")
    .setInputCols(Array("sentence_embeddings")) // "document"
    .setOutputCol("class")

  val fr_sentiment_pipeline =
    new Pipeline().setStages(Array(document, embeddings, sentimentClassifier))

  val input = new data(spark).example
  val light_pipeline = new LightPipeline(
    fr_sentiment_pipeline.fit(Seq[String]().toDF("text"))
  )
  val r = light_pipeline.transform(input)
  r.printSchema()
  r.show(false)

}

object EntityScratch {
  // TODO
  // (sp) https://nlp.johnsnowlabs.com/2021/10/03/xlm_roberta_base_token_classifier_conll03_en.html
  // https://nlp.johnsnowlabs.com/models?edition=Spark+NLP+3.3&language=en&q=robert
}
