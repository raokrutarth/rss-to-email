package models

import play.api.libs.json._
import java.time.LocalDate
import java.security.MessageDigest
import java.math.BigInteger

case class FeedURL(url: String) {

  def digest(): String = {
    // get a query-safe identifier for the url
    String.format(
      "%032x",
      new BigInteger(
        1,
        MessageDigest
          .getInstance("SHA-256")
          .digest(url.getBytes("UTF-8"))
      )
    )
  }
}
case class FeedContent(url: String, processed: Boolean = false)

case class Feed(
    url: FeedURL,
    content: Option[Seq[FeedContent]],
    lastScraped: Option[LocalDate]
)

// conmpanion objects needed for json parsing to object
object FeedURL {
  implicit val feedUrlFormat = Json.format[FeedURL]
}

object FeedContent {
  implicit val feedContentFormat = Json.format[FeedContent]
}

object Feed {
  implicit val feedFormat = Json.format[Feed]
}
