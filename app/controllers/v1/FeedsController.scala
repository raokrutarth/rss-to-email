package controllers.v1

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
import models.FeedURL
import models.Feed
import models.FeedContent
import java.time.LocalDate
import akka.actor
import play.api.Logger
// import core.FeedValidator
import core.{Scraper, Analysis}

@Singleton
class FeedsController @Inject() (
    val controllerComponents: ControllerComponents,
    val analysis: Analysis
) extends BaseController {

  val log: Logger = Logger(this.getClass())

  def getReport() = Action(parse.json) { request =>
    (request.body \ "urls").asOpt[Seq[String]] match {
      case Some(urls) =>
        log
          .info(s"Report requested for ${urls.size} feeds: $urls")
        /*
        - fetch  XML content.
        - filter by content lookback date.
        - extract main entities from headings.
        - extract main words from content.
        - find ratio of negative to positive headings.
        - negative to positive
        - send report to email and return OK
        - report contains new articles with links.
         */
        val allContents: Seq[Seq[FeedContent]] = urls
          .flatMap(u => Scraper.getContent(FeedURL(u)))

        val report = {
          analysis.generateReport(allContents.flatten)
          Ok("Urls found")
        }
        report
      case _ =>
        BadRequest("No URLs for RSS feeds found in request")
    }
  }
}
