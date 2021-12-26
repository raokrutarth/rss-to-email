package controllers

import configuration.AppConfig
import play.api._
import play.api._
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
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

@Singleton
class RSSController @Inject() (
    val controllerComponents: ControllerComponents,
    val db: DocumentStore
) extends BaseController {
  val logger: Logger = Logger(this.getClass())

  def triggerReport(lookback: String) = Action {
    implicit request: Request[AnyContent] =>
      /*
      - get all URLs from DB.
      - fetch  XML content.
      - filter by content lookback date.
      - extract main entities from headings.
      - extract main words from content.
      - find ratio of negative to positive headings.
      - find ratio of negative to positive content.
      - send report to email and return OK
        - report contains new articles with links.
       */
      val insertRes = db.upsertFeed(Feed(FeedURL("abc-test.com"), None, None))
      logger.info(s"Upsert returned $insertRes")

      val dbUrl: Try[Seq[FeedURL]] = db.getFeedURLs()
      dbUrl match {
        case Success(urls) =>
          Ok(
            s"lookback: $lookback got urls $urls"
          )
        case Failure(exception) =>
          InternalServerError("Unable to get URLs for current user")
      }
  }

  def listURLs() = Action { implicit request: Request[AnyContent] =>
    Ok("list of urls")
  }

  def addURL() = Action(parse.json) { request =>
    val reqData: JsValue = request.body
    val url = (reqData \ "url").as[String]
    Ok(
      s"added $url"
    )
  }

  def removeURL() = Action(parse.json) { request =>
    val reqData: JsValue = request.body
    val url = (reqData \ "url").as[String]
    Ok(
      s"removed $url"
    )
  }
}
