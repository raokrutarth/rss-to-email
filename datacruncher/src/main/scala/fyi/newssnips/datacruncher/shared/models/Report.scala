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
