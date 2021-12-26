package controllers.v1

import configuration.AppConfig
import play.api._
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.json._
import play.api.mvc._
import play.libs.Akka
import scala.util.{Failure, Success, Try}
import java.io._
import java.lang.Runnable
import javax.inject.Inject
import javax.inject.Named
import javax.inject._
import fyi.newssnips.models.{AnalysisRow, FeedContent, FeedURL}
import java.time.LocalDate
import akka.actor
import play.api.Logger
import fyi.newssnips.datastore.DatastaxCassandra

import DatastaxCassandra.spark.implicits._

@Singleton
class FeedsController @Inject() (
    val controllerComponents: ControllerComponents
) extends BaseController {

  val log: Logger = Logger(this.getClass())

  def hp() = Action { implicit request: Request[AnyContent] =>
    val homePageTable = "home_page_analysis_results"

    DatastaxCassandra.getDataframe(homePageTable) match {
      case Success(df) =>
        val resDf = df.sort($"totalNumTexts".desc).as[AnalysisRow]
        log.info(s"Found home page results from table $homePageTable.")
        resDf.show()
        log.info(s"Parsing ${resDf.count()} rows into HTML template.")

        // TODO progress bar for sentiment scale
        // https://www.w3schools.com/bootstrap/bootstrap_progressbars.asp
        Ok(
          views.html.index(
            resDf.collect(),
            Seq("rss://a.com", "rss://b.com")
          )
        ).as("text/html")

      case _ => InternalServerError("A server error occurred: ")
    }
  }

  def getReport() = Action(parse.json) { request =>
    (request.body \ "urls").asOpt[Seq[String]] match {
      case Some(urls) =>
        log
          .info(s"Report requested for ${urls.size} feeds: $urls")
        Ok("ok")
      case _ =>
        BadRequest("No URLs for RSS feeds found in request")
    }
  }
}
