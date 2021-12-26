package core
import scala.util.{Try, Success, Failure}
import models.FeedURL
import models.Feed
import core.Scraper
import play.api.Logger

// object FeedValidator {
//   val log: Logger = Logger(this.getClass())

//   def isValid(feedUrl: FeedURL): Boolean = {
//     log.info(s"Checking the validity of feed $feedUrl")
//     // scrape the xml feed and make sure it's a reachable address
//     Scraper.getContent(Feed(feedUrl, None, None)) match {
//       case Some(_) => true
//       case _       => false
//     }
//   }
// }
