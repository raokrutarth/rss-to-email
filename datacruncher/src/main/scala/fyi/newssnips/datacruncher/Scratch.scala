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

// loading models from HF (see colabs)
// https://nlp.johnsnowlabs.com/docs/en/transformers

class data(spark: SparkSession) {
  import spark.implicits._
  val example = Seq(
    "Wyoming GOP votes to no longer recognize Rep. Liz Cheney as a Republican.", // neg
    "Aung San Suu Kyi being treated well: Myanmar army (BBC).", // pos
    "Jeremy was positive but the federal reserver thought people are wrong.", // neg or neutral
    "Plus, Chris talks about the battle for the living room with NYU Professor Scott Galloway, author of The Four: The Hidden DNA of Amazon, Apple, Facebook, and Google.", // neutral
    "Google has its best week in more than ten years.",              // pos
    "Is Amazon's new venture with Google a serious threat to Apple?", // neg
    "Covid-19 Surge Prompts Renewed Lockdown in Parts of Europe",
    "Biden Says U.S. Weighing Diplomatic Boycott of Beijing Olympics",
    
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

  val pipeline =
    new PretrainedPipeline("analyze_sentimentdl_use_twitter", lang = "en")

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
  // https://nlp.johnsnowlabs.com/models?language=en&edition=Spark+NLP+3.3&q=Recognize+Entities
  // https://nlp.johnsnowlabs.com/2021/08/05/distilbert_base_token_classifier_ontonotes_en.html
  // https://nlp.johnsnowlabs.com/2021/09/26/roberta_base_token_classifier_ontonotes_en.html

  // https://nlp.johnsnowlabs.com/2021/03/23/onto_recognize_entities_bert_mini_en.html
  // https://nlp.johnsnowlabs.com/2021/03/23/onto_recognize_entities_electra_base_en.html
  // https://nlp.johnsnowlabs.com/2021/09/26/distilroberta_base_token_classifier_ontonotes_en.html
  // https://nlp.johnsnowlabs.com/2021/10/03/xlm_roberta_base_token_classifier_ontonotes_en.html
}
