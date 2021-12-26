package fyi.newssnips.datacruncher.scripts

import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.SparkSession
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.nlp.DocumentAssembler
import com.johnsnowlabs.nlp.LightPipeline
import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.PerformanceUtils
import fyi.newssnips.datacruncher.utils.DfUtils
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

// all posts:
// https://github.com/JohnSnowLabs/spark-nlp/tree/5aa81538b04c7cd1eb7b00fbfb154ea6906cf2d5/docs/_posts

object Init {
  val log = Logger("app." + this.getClass().toString())

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

  import spark.implicits._

  lazy val exampleDf = {
    log.info(s"Sample contents df:")

    val df = Seq(
      (
        "Wyoming GOP votes to no longer recognize Rep. Liz Cheney as a Republican.",
        "neg"
      ),
      (
        "This is wonderful.",
        "pos"
      ),
      (
        "This is horrible.",
        "neg"
      ),
      (
        "This is a house.",
        "neutral"
      ),
      ("Aung San Suu Kyi being treated well: Myanmar army (BBC).", "pos"),
      (
        "Jeremy was positive but the federal reserver thought people are wrong.",
        "neg/neutral"
      ),
      (
        "Plus, Chris talks about the battle for the living room with NYU Professor Scott Galloway, author of The Four: The Hidden DNA of Amazon, Apple, Facebook, and Google.",
        "neg/neutral"
      ),
      ("Google has its best week in more than ten years.", "pos"),
      ("Is Amazon's new venture with Google a serious threat to Apple?", "neg"),
      ("Covid-19 Surge Prompts Renewed Lockdown in Parts of Europe", "neg"),
      (
        "Biden Says U.S. Weighing Diplomatic Boycott of Beijing Olympics",
        "neg"
      ),
      (
        "How Hunter Biden's Firm Helped Secure Cobalt for the Chinese.",
        "neutral"
      ),
      (
        "Paris Hilton says mom Kathy 'changes the subject' when she talks about alleged boarding school abuse, hasn't watched documentary.",
        "neg"
      ),
      (
        "Young Dolph's longtime girlfriend shares heartbreaking post after rapper's fatal shooting",
        "neg"
      ),
      (
        "Shawn Mendes and Camila Cabello breakup details: 'The romance just fizzled'",
        "neg"
      ),
      (
        "Scarlett Johansson steps out in sparkly corset in first red carpet appearance since giving birth",
        "neutral"
      ),
      ("Cathie Wood Pulls the Trigger on These 3 'Strong Buy' Stocks", "pos"),
      (
        "Michael Burry dumped just about everything in Q3 to guard against the 'mother of all crashes' — but he did purchase 3 interesting new holdings",
        "neg/neutral"
      ),
      (
        "Britney Spears slams Christina Aguilera for 'refusing to speak' on conservatorship: 'Yes I do matter!'",
        "neg"
      )
    ).toDS.toDF("text", "actual")
    df.show()
    df
  }

  lazy val contentsDf = {
    log.info("Loading contents df: ")
    val df = spark.read.parquet("contents.parquet")
    df.show()
    df
  }

  def checkMem(df: DataFrame) = {
    log.info("Memory before sample.")
    PerformanceUtils.logMem()

    DfUtils.showSample(df, 8f, overrideEnv = true)

    log.info("labeling:")
    // df.select("text", "sentiment").show(false)
    DfUtils.showSample(
      df.select(
        col("text"),
        col("sentiment").getItem(0).getField("result")
        // col("actual")
      ),
      20f,
      overrideEnv = true,
      truncate = 150
    )

    log.info("Memory after sample.")
    PerformanceUtils.logMem()
  }
}

/*
    sentiment scores:
        "bert_base_sequence_classifier_imdb" 6/6
        "bert_sequence_classifier_finbert" 4/6 (neutral too often)
        sent_bert_wiki_books_sst2 (neutral more often. wrong. can try with other embeddings.)
        bert_large_sequence_classifier_imdb ( 6 gb memory usage)
        analyze_sentimentdl_use_twitter (5/6 6gb memory usage)

    to try:
      https://nlp.johnsnowlabs.com/docs/en/annotators#sentimentdl
 */
object SentimentSst2 {
  // light: 800M pre sample, 7.4GB peak, 2m20s runtime
  // document shrink: 1 Gb presample, 7.3 GB peak, 2m30s runtime
  import Init.spark.implicits._

  val pipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document")
            .setCleanupMode("shrink_full"),
          BertSentenceEmbeddings
            .pretrained("sent_bert_wiki_books_sst2", "en")
            .setInputCols(Array("document"))
            .setOutputCol("sentence_embeddings"),
          ClassifierDLModel
            .pretrained("classifierdl_bertwiki_finance_sentiment", "en")
            .setInputCols(Array("sentence_embeddings"))
            .setOutputCol("sentiment")
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.exampleDf))
}

// loading models from HF (see colabs)
// https://nlp.johnsnowlabs.com/docs/en/transformers#compatibility
// https://huggingface.co/facebook/bart-large-cnn (see other models by org)
// verify latest supported: https://github.com/JohnSnowLabs/spark-nlp/tree/master/src/main/scala/com/johnsnowlabs/nlp/annotators/classifier/dl
object SentimentLoadDistillBert {
  /*
// https://huggingface.co/typeform/distilbert-base-uncased-mnli (interesting model)
  https://huggingface.co/bhadresh-savani/distilbert-base-uncased-emotion

  CouchCat/ma_sa_v7_distil: 930m ps, 3.3GB peak, 45s runtime, (7/19)
  distilbert-base-uncased-finetuned-sst-2-english:
    850m presample, 3.4G, everything positive. (10/19), [5m runtime, possible winner]
   */
  import Init.spark.implicits._

  val pipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document"),
          new Tokenizer()
            .setInputCols("document")
            .setOutputCol("token"),
          DistilBertForTokenClassification
            .loadSavedModel(
              "distilbert-base-uncased-finetuned-sst-2-english/saved_model/1",
              Init.spark
            )
            .setInputCols(Array("document", "token"))
            .setOutputCol("sentiment")
            .setCaseSensitive(false)
            .setMaxSentenceLength(256)
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.contentsDf))
}

object SentimentLoadBert {
  /*
  M-FAC/bert-mini-finetuned-sst2 (l1=pos, l0=neg):
    500M presample, 3.2 Gb peak, 40s runtime, (11/19) sp, slight pos risk,
    [not accurate enough for winner]

  barissayil/bert-sentiment-analysis-sst: shit
   */

  import Init.spark.implicits._

  val pipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document"),
          new Tokenizer()
            .setInputCols("document")
            .setOutputCol("token"),
          BertForTokenClassification
            .loadSavedModel(
              "M-FAC/bert-mini-finetuned-sst2/saved_model/1",
              Init.spark
            )
            .setInputCols(Array("document", "token"))
            .setOutputCol("sentiment")
            .setCaseSensitive(true)
            .setMaxSentenceLength(128)
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.contentsDf))
}

object SentimentLoadRoberta {
  /*
  cardiffnlp/bertweet-base-sentiment (not working, not exact roberta)

  cardiffnlp/twitter-roberta-base-sentiment:
    1.3G ps, 4.4Gb peak, 1m runtime, (10/19), [10m, extreme positive skew]

  j-hartmann/emotion-english-distilroberta-base: [3.7G 5m]

  cardiffnlp/twitter-roberta-base-emotion:
    [4.3Gb peak, joy skew, 10m]

  siebert/sentiment-roberta-large-english: 7GB+ peak. crashed. most likely accurate.
  mrm8488/distilroberta-finetuned-financial-news-sentiment-analysis:
    900M ps, 3.5G peak, too-positive
  mrm8488/distilroberta-finetuned-rotten_tomatoes-sentiment-analysis:
    950m ps, 3.7 G peak, 43s run, random
   */
  // 0=neg, 1=pos, 2=neut
  import Init.spark.implicits._

  val pipeline = new LightPipeline(
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
            .loadSavedModel(
              "cardiffnlp/twitter-roberta-base-emotion/saved_model/1",
              Init.spark
            )
            .setInputCols(Array("document", "token"))
            .setOutputCol("sentiment")
            .setCaseSensitive(true)
            .setMaxSentenceLength(256)
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.contentsDf))
}

object SentimentLoadXmlRoberta {
  /*
  cardiffnlp/twitter-xlm-roberta-base-sentiment: 2.6G ps, 6.6G peak (9/19)
   */

  import Init.spark.implicits._

  val pipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document"),
          new Tokenizer()
            .setInputCols("document")
            .setOutputCol("token"),
          XlmRoBertaForTokenClassification
            .loadSavedModel(
              "cardiffnlp/twitter-xlm-roberta-base-sentiment/saved_model/1",
              Init.spark
            )
            .setInputCols(Array("document", "token"))
            .setOutputCol("sentiment")
            .setCaseSensitive(true)
            .setMaxSentenceLength(128)
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.exampleDf))
}

object SentimentSst {
  // light: 800M pre sample, 7.4GB peak, 2m20s runtime
  // document shrink: 1 Gb presample, 7.3 GB peak, 2m30s runtime
  import Init.spark.implicits._

  val pipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document")
            .setCleanupMode("shrink_full"),
          BertSentenceEmbeddings
            .pretrained("sent_bert_wiki_books_sst2", "en")
            .setInputCols(Array("document"))
            .setOutputCol("sentence_embeddings"),
          ClassifierDLModel
            .pretrained("classifierdl_bertwiki_finance_sentiment", "en")
            .setInputCols(Array("sentence_embeddings"))
            .setOutputCol("sentiment")
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.exampleDf))
}

object SentimentPretrained {
  // analyze_sentimentdl_use_twitter: 2.5GB preaample, 7GB peak
  // analyze_sentimentdl_use_imdb: 2.5 gb presample, 7.1GB peak, 4m runtime
  // analyze_sentimentdl_glove_imdb: too slow (hours) crashed.
  // analyze_sentiment: 630M presample, 2.6G, 32s runtime

  val pipeline =
    new LightPipeline(
      new PretrainedPipeline(
        "analyze_sentiment",
        lang = "en"
      ).model
    )

  Init.checkMem(pipeline.transform(Init.exampleDf))
}

object SentimentBert {
  /*
  imdb: non-light: 1.1G pre sample, 4.5G peak
  imdb: light: 1.1gb pre-smaple, 4.1gb peak, 47s runtime

  fibert-light: 1.1g presample, 3.8G peak, 48s runtime
   */

  import Init.spark.implicits._

  val model = "bert_sequence_classifier_finbert"
  // val model = "bert_base_sequence_classifier_imdb"

  val pipeline = new LightPipeline(
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
            .pretrained(model, "en")
            .setInputCols("document", "token")
            .setOutputCol("sentiment")
            .setCaseSensitive(true)
            .setMaxSentenceLength(512)
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.exampleDf))
}

object SentimentEmbd {
  /*
  TODO need to train for the combination to work. Have to gather training data.
   */
  import Init.spark.implicits._

  val pipeline = new LightPipeline(
    new Pipeline()
      .setStages(
        Array(
          new DocumentAssembler()
            .setInputCol("text")
            .setOutputCol("document"),
          new Tokenizer()
            .setInputCols("document")
            .setOutputCol("token"),
          XlmRoBertaSentenceEmbeddings
            .pretrained()
            .setInputCols(Array("document"))
            .setOutputCol("sentence_embeddings"),
          SentimentDLModel
            .pretrained("sentimentdl_use_twitter")
            .setInputCols("sentence_embeddings")
            .setThreshold(0.7f)
            .setOutputCol("sentiment")
        )
      )
      .fit(Seq[String]().toDF("text"))
  )

  Init.checkMem(pipeline.transform(Init.exampleDf))
}

object SentimentStanford {
  /*
  https://shekhargulati.com/2017/09/30/sentiment-analysis-in-scala-with-stanford-corenlp-2/
  https://developer.hpe.com/blog/spark-streaming-and-twitter-sentiment-analysis/
  https://stanfordnlp.github.io/CoreNLP/sentiment.html
  https://nlp.stanford.edu/sentiment/
  https://databricks-prod-cloudfront.cloud.databricks.com/public/4027ec902e239c93eaaa8714f173bcfc/1233855/3233914658600709/588180/latest.html
  https://github.com/harpribot/corenlp-scala-examples/blob/master/src/main/scala/com/harpribot/corenlp_scala/SentimentAnalysisExample.scala
  https://www.programcreek.com/scala/edu.stanford.nlp.pipeline.StanfordCoreNLP


   */
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
