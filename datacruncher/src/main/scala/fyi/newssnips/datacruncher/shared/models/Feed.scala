package fyi.newssnips.models

import java.security.MessageDigest
import java.math.BigInteger
import java.time.OffsetDateTime

//
case class FeedURL(value: String) {

  def digest(): String = {
    // get a query-safe identifier for the url
    String.format(
      "%032x",
      new BigInteger(
        1,
        MessageDigest
          .getInstance("SHA-256")
          .digest(value.getBytes("UTF-8"))
      )
    )
  }
}
case class FeedContent(
    url: String,
    title: String,
    body: String,
    processed: Boolean = false
)

case class Feed(
    title: String,
    url: FeedURL,
    content: Seq[FeedContent],
    // https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html
    lastScraped: OffsetDateTime
)

/** As present in the df
  */
case class FeedRow(
    feed_id: Long,
    url: String,
    title: String,
    last_scraped: String
)

case class TextsPageRow(
    text: String,
    url: String
)
