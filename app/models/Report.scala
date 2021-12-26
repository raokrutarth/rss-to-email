package models

import play.api.libs.json.Json

case class AnalysisRow(
    entityName: Option[String],
    entityType: Option[String],
    sentiment: Option[String],
    // texts: Option[Array[String]],
    numTexts: Option[Long],
    aggregateConfidence: Option[Double]
)
// companion objects needed for json parsing to object
// object Report {
//   implicit val reportFormat = Json.format[Report]
// }
