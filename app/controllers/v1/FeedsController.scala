package controllers.v1

import configuration.AppConfig
import play.api._
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.json._
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

import core.{Scraper, Analysis}
import models.AnalysisRow

@Singleton
class FeedsController @Inject() (
    val controllerComponents: ControllerComponents,
    val analysis: Analysis
) extends BaseController {

  val log: Logger = Logger(this.getClass())

  def hp() = Action { implicit request: Request[AnyContent] =>
    // val allContents: Seq[Seq[FeedContent]] =
    //   Seq(
    //     "http://rss.cnn.com/rss/cnn_topstories.rss"
    //     // "http://rss.cnn.com/rss/cnn_world.rss",
    //     // "http://rss.cnn.com/rss/cnn_us.rss",
    //     // "http://rss.cnn.com/rss/cnn_latest.rss",
    //     // "https://rss.politico.com/congress.xml",
    //     // "http://rss.politico.com/politics.xml",
    //     // "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
    //     // "http://feeds.feedburner.com/zerohedge/feed",
    //     // "http://thehill.com/rss/syndicator/19110",
    //     // "http://thehill.com/taxonomy/term/1778/feed",
    //     // "https://nypost.com/feed",
    //     // "https://snewsi.com/rss"
    //     // "https://www.reddit.com/r/StockMarket/.rss"
    //   )
    //     .flatMap(u => Scraper.getContent(FeedURL(u)))

    // val reportDf = analysis.generateReport(allContents.flatten)
    // val analysisRows = reportDf

    // log.info(s"Parsing ${analysisRows.size} rows into HTML template.")

    // TODO progress bar for sentiment scale
    // https://www.w3schools.com/bootstrap/bootstrap_progressbars.asp
    Ok(
      views.html.index(
        // analysisRows
        Array(
          AnalysisRow(
            Some("et"),
            Some("ORD"),
            Some("negative"),
            Some(0),
            Some(0)
          ),
          AnalysisRow(
            Some("et3333333"),
            Some("ORD"),
            Some("positive"),
            Some(100000),
            Some(0.999)
          )
        )
      )
    ).as("text/html")
  }

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
