package fyi.newssnips.models

case class AnalysisRow(
    entityName: Option[String],
    entityType: Option[String],
    negativeMentions: Option[Long],
    positiveMentions: Option[Long],
    positiveTextIds: Option[Array[Long]],
    negativeTextIds: Option[Array[Long]],
    totalNumTexts: Option[Long],
    aggregateConfidence: Option[Double]
)
