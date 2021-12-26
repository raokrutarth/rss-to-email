package fyi.newssnips.datacruncher.models

case class AnalysisRow(
    entityName: Option[String],
    entityType: Option[String],
    sentiment: Option[String],
    // texts: Option[Array[String]],
    numTexts: Option[Long],
    aggregateConfidence: Option[Double]
)

// companion objects needed for json parsing to object
// object AnalysisRow {
//   implicit val analysisRowFormat = Json.format[AnalysisRow]
// }
