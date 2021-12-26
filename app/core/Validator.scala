package core
import scala.util.{Try, Success, Failure}
import models.FeedURL
import models.Feed

object FeedValidator {

  def isValid(feedUrl: FeedURL): Boolean = {
    // scrape the xml feed and make sure it's a reachable address
    true
  }
}
