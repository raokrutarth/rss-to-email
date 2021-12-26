package fyi.newssnips.models

case class AnalysisRow(
    entityName: String,
    entityType: String,
    negativeMentions: Long,
    positiveMentions: Long,
    positiveTextIds: Option[List[Long]],
    negativeTextIds: Option[List[Long]],
    totalNumTexts: Long
)

// FIXME use traits and proper inheretence
case class AnalysisRowUi(
    entityName: String,
    entityType: String,
    negativeMentions: Long,
    positiveMentions: Long,
    totalNumTexts: Long,
    positivityScore: Int // calculated at query time
)
