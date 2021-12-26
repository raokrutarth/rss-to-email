package fyi.newssnips.shared

case class CategoryDbMetadata(
    name: String,
    analysisTableName: String, // table name of sentiment and count of mentions
    textsTableName: String,    // table name of raw texts for the category
    sourceFeedsTableName: String, // table name of rss feed URLs
    articleUrlsTableName: String, // table name of links to article/post of sentence
    lastUpdateKey: String // key of the last updated in KV table
)

object DbConstants {
  val categoryToDbMetadata: Map[String, CategoryDbMetadata] =
    Seq("home", "markets", "politics", "entertainment")
      .map(category =>
        (
          category,
          CategoryDbMetadata(
            name = category,
            analysisTableName = s"${category}_page_analysis_results",
            textsTableName = s"${category}_page_texts",
            sourceFeedsTableName = s"${category}_page_feeds",
            articleUrlsTableName = s"${category}_page_urls",
            lastUpdateKey = s"${category}_page_analysis_last_updated"
          )
        )
      )
      .toMap
}

object EntityTypeDescriptions {
  /* https://nlp.johnsnowlabs.com/2021/08/05/bert_large_token_classifier_ontonote_en.html#predicted-entities */
  /* https://notebook.community/rishuatgithub/MLPy/nlp/UPDATED_NLP_COURSE/02-Parts-of-Speech-Tagging/02-NER-Named-Entity-Recognition */
  // https://texthero.org/docs/api/texthero.nlp.named_entities
  val descriptions = Map(
    "PERSON"      -> "People, including fictional.",
    "NORP"        -> "Nationalities or religious or political groups.",
    "FAC"         -> "Buildings, airports, highways, bridges, etc.",
    "ORG"         -> "Companies, agencies, institutions, etc.",
    "GPE"         -> "Countries, cities, states.",
    "LOC"         -> "Non-GPE locations, mountain ranges, bodies of water.",
    "PRODUCT"     -> "Objects, vehicles, foods, etc. (Not services.)",
    "EVENT"       -> "Named hurricanes, battles, wars, sports events, etc.",
    "WORK_OF_ART" -> "Titles of books, songs, etc.",
    "LAW"         -> "Named documents made into laws.",
    "LANGUAGE"    -> "Any named language.",
    "DATE"        -> "Absolute or relative dates or periods.",
    "TIME"        -> "Times smaller than a day.",
    "PERCENT"     -> "Percentage, including %",
    "MONEY"       -> "Monetary values, including unit.",
    "QUANTITY"    -> "Measurements, as of weight or distance.",
    "ORDINAL"     -> "first, second, etc.",
    "CARDINAL"    -> "Numerals that do not fall under another type."
  )
}
