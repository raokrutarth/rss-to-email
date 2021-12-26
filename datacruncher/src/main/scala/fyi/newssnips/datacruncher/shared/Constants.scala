package fyi.newssnips.shared

case class CategoryDbMetadata(
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
