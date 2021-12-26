package fyi.newssnips.models

import play.api.libs.json._
import fyi.newssnips.core.CategoryAnalysisPageData

object Formatters {
  implicit val feedRowFormat     = Json.format[FeedRow]
  implicit val analysisRowFormat = Json.format[AnalysisRow]
  implicit val pdRowFormat       = Json.format[CategoryAnalysisPageData]
}
