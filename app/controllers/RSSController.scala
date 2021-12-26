package controllers

import configuration.AppConfig
import play.api._
import play.api._
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc._
import play.libs.Akka
import scala.util.{Try, Success, Failure}
import java.io._
import java.lang.Runnable
import javax.inject.Inject
import javax.inject.Named
import javax.inject._
import datastore.DocumentStore
import models.FeedURL
import models.Feed
import models.FeedContent
import java.time.LocalDate
import akka.actor
import play.api.Logger
import core.FeedValidator
import core.{Scraper, Analysis}

@Singleton
class RSSController @Inject() (
    val controllerComponents: ControllerComponents,
    val db: DocumentStore
) extends BaseController {

  val logger: Logger = Logger(this.getClass())

  def addURL() = Action(parse.json) { request =>
    val feedUrl: FeedURL = request.body.as[FeedURL]

    if (FeedValidator.isValid(feedUrl)) {
      val uTry = db.upsertFeed(
        Feed(feedUrl, None, None)
      )
      uTry match {
        case Success(_) =>
          Ok(
            Json.obj("message" -> s"${feedUrl.url} added.")
          )
        case Failure(_) =>
          ServiceUnavailable(s"Unable to save ${feedUrl.url}")
      }
    } else
      BadRequest(s"${feedUrl.url} is not a valid RSS feed URL.")
  }

  def listURLs() = Action { implicit request: Request[AnyContent] =>
    val feedsTry: Try[Seq[Feed]] = db.getFeeds()
    feedsTry match {
      case Success(feeds) =>
        Ok(
          Json.obj("urls" -> feeds.map(f => f.url.url))
        )
      case Failure(exception) =>
        InternalServerError("Unable to get URLs for current user")
    }
  }

  def triggerReport(lookback: String) = Action {
    implicit request: Request[AnyContent] =>
      /*
      - get all URLs from DB.
      - fetch  XML content.
      - filter by content lookback date.
      - extract main entities from headings.
      - extract main words from content.
      - find ratio of negative to positive headings.
      - negative to positive
      - send report to email and return OK
      - report contains new articles with links.
       */
      val userId = db.getUser()
      logger.info(
        s"Generating feeds report for user $userId with lookback $lookback"
      )
      val feedsTry: Try[Seq[Feed]] = db.getFeeds(userId)
      val reports: Seq[String] = feedsTry match {
        case Success(feeds) =>
          feeds
            .map(f => (f, Scraper.getContent(f)))
            .map {
              case (feed, Some(contents)) =>
                Analysis.getReport(contents, feed)
              case (feed, _) =>
                logger.error(s"Unable to get contents for feed $feed")
            }
          Seq()
        case Failure(exception) =>
          InternalServerError("Unable to get URLs for current user")
          Seq()
      }
      Ok(
        s"Generated reports $reports"
      )
  }

  def removeURL() = Action(parse.json) { request =>
    val feedUrl: FeedURL = request.body.as[FeedURL]
    db.deleteFeed(feedUrl) match {
      case Success(true) =>
        Ok(
          Json.obj("deleted" -> true)
        )
      case Success(false) => NotFound(s"${feedUrl.url} does not exist")
      case Failure(exception) =>
        logger.error(s"Unable to remove feed $feedUrl with error $exception")
        ServiceUnavailable("Unable to delete feed")
    }

  }
}
