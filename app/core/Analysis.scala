package core

import models.{Feed, FeedURL, FeedContent}
import play.api.Logger

object Analysis {
  val log: Logger = Logger(this.getClass())

  def getReport(contents: Seq[FeedContent], feed: Feed) {
    log.info(s"Performing analysis for content $contents")
    None
  }
}
