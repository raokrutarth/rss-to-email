package fyi.newssnips.models

case class AnalysisRow(
    entityName: Option[String],
    entityType: Option[String],
    negativeMentions: Option[Long],
    positiveMentions: Option[Long],
    positiveTextIds: Option[List[Long]],
    negativeTextIds: Option[List[Long]],
    totalNumTexts: Option[Long],
    aggregateConfidence: Option[Double]
)
