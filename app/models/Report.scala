package models

import play.api.libs.json.Json
import models.FeedURL

object Sentiment extends Enumeration {
  // TODO use
  type WeekDay = Value
  val Positive, Negative = Value
}

case class Report(
    url: FeedURL,
    commonEntities: Map[String, Int],
    sentiment: Map[String, Float]
)

// companion objects needed for json parsing to object
object Report {
  implicit val reportFormat = Json.format[Report]
}
