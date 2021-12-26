package fyi.newssnips.webapp.models

import java.util.Date
import play.api.libs.json.JsValue

object Frequency extends Enumeration {
  val Weekly, Monthly, Never = Value

  def fromString(s: String): Frequency.Value = {
    values.find(_.toString.toLowerCase() == s.toLowerCase()).get
  }
}

case class SubscriberRow(
    email: String,
    frequency: String,
    createdAt: Option[Date],
    lastDigestAt: Option[Date],
    metadata: Option[JsValue]
) {
  require(Frequency.values.map(_.toString()).contains(frequency))
}

case class SignupData(email: String, frequency: String) {
  require(Frequency.values.map(_.toString()).contains(frequency))
}
