package controllers.v1

import play.api._
import play.api.mvc._
import scala.util.{Failure, Success}
import javax.inject.Inject
import javax.inject._
import play.api.Logger
import fyi.newssnips.datastore.DatastaxCassandra
import play.api.inject.ApplicationLifecycle
import fyi.newssnips.webapp.datastore.Cache
import scala.concurrent.Future
import fyi.newssnips.shared.DbConstants
import fyi.newssnips.core.PageDataFetcher
import play.api.cache.Cached
import configuration.AppConfig

@Singleton
class FeedsController @Inject() (
    val controllerComponents: ControllerComponents,
    lifecycle: ApplicationLifecycle,
    cached: Cached
) extends BaseController {

  private val log: Logger = Logger("app." + this.getClass().toString())
  private val cache       = new Cache(DatastaxCassandra.spark)
  private val dataFetcher = new PageDataFetcher(cache)
  private val errResp = InternalServerError(
    "<h1>A server error occurred: Please try again later.</h1>"
  ).as("text/html")

  val pageCacheTimeSec: Int = if (AppConfig.settings.inProd) 1800 else 300

  def home() = cached.status(_ => "homeAnalysisPage", 200, pageCacheTimeSec) {
    Action { implicit request: Request[AnyContent] =>
      val dbMetadata = DbConstants.categoryToDbMetadata("home")
      log.info(
        s"Received home page request from client ${request.remoteAddress}. " +
          s"Using db metadata ${dbMetadata.toString()}"
      )
      dataFetcher.getCategoryAnalysisPage(dbMetadata) match {
        case Success(data) =>
          log.info(
            s"Parsing ${data.analysisRows.size} analysis row(s) and " +
              s"${data.sourceFeeds.size} feed(s) into HTML template."
          )
          // TODO progress bar for sentiment scale
          // https://www.w3schools.com/bootstrap/bootstrap_progressbars.asp

          Ok(
            views.html.analysisPage(
              data.analysisRows,
              data.sourceFeeds,
              data.lastUpdated,
              "home"
            )
          ).as("text/html")

        case Failure(exc) =>
          log.error(s"Unable to get home page with exception $exc")
          errResp
      }
    }
  }

  def category(categoryId: String) =
    cached.status(_ => "category" + categoryId, 200, pageCacheTimeSec) {
      Action { implicit request: Request[AnyContent] =>
        log.info(
          s"Received category ${categoryId} page request from client ${request.remoteAddress}"
        )
        DbConstants.categoryToDbMetadata get categoryId match {
          case Some(dbMetadata) => {
            log.info(
              s"Using db metadata ${dbMetadata.toString()} for category $categoryId."
            )
            dataFetcher.getCategoryAnalysisPage(dbMetadata) match {
              case Success(data) =>
                log.info(
                  s"Parsing ${data.analysisRows.size} analysis row(s) and ${data.sourceFeeds.size} feed(s) into HTML template."
                )
                Ok(
                  views.html.analysisPage(
                    data.analysisRows,
                    data.sourceFeeds,
                    data.lastUpdated,
                    categoryId
                  )
                ).as("text/html")

              case Failure(exc) =>
                log.error(s"Unable to get $categoryId page with exception $exc")
                errResp
            }
          }
          case _ => BadRequest(s"$categoryId is not a valid category.")
        }
      }
    }

  def mentions(categoryId: String, entityName: String, entityType: String, sentiment: String) =
    cached.status(
      _ => "mentions" + categoryId + entityName + entityType + sentiment,
      200,
      pageCacheTimeSec
    ) {
      Action { implicit request: Request[AnyContent] =>
        log.info(
          s"Serving request from ${request.remoteAddress} for $categoryId entity " +
            s"${entityName} and type ${entityType} and sentiment ${sentiment}."
        )
        DbConstants.categoryToDbMetadata get categoryId match {
          case Some(dbMetadata) =>
            dataFetcher.getTextsPage(dbMetadata, entityName, entityType, sentiment) match {
              case Success(pageData) =>
                Ok(
                  views.html.textsPage(pageData.rows, entityName, entityType, sentiment)
                ).as("text/html")

              case Failure(exc) =>
                log.error(s"Failed to get texts page with exception $exc")
                errResp
            }

          case _ => BadRequest(s"$categoryId is not a valid category ID.")
        }
      }
    }

  lifecycle.addStopHook { () =>
    Future.successful {
      log.warn("Running feeds controller EOL hook.")
      DatastaxCassandra.cleanup()
    }
  }
}
