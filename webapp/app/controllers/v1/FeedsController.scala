package controllers.v1

import play.api._
import play.api.mvc._
import scala.util.{Failure, Success}
import javax.inject.Inject
import javax.inject._
import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared._
import fyi.newssnips.core.PageDataFetcher
import play.api.cache.Cached
import play.twirl.api.Html
import fyi.newssnips.datastore.Cache
import fyi.newssnips.webapp.config.AppConfig
import fyi.newssnips.webapp.core.books.Books
import fyi.newssnips.core.CategoryAnalysisPageData
import scala.collection.mutable.LinkedHashMap

@Singleton
class FeedsController @Inject() (
    val controllerComponents: ControllerComponents,
    cached: Cached,
    cache: Cache,
    dal: PageDataFetcher
) extends BaseController {

  private val log: Logger = Logger("app." + this.getClass().toString())
  private val errResp = InternalServerError(
    views.html.siteTemplate("Error")(
      Html(
        """
          <div class='alert alert-danger col-md-6 offset-md-3'>
          <h3>An unknown server error occurred. Please try again later.</h3></div>
        """
      )
    )
  ).as("text/html")

  val pageCacheTimeSec: Int = if (AppConfig.settings.shared.inProd) 120 else 5

  def home(positivity: Int) =
    cached.status(_ => "home" + s"$positivity", status = 200, pageCacheTimeSec) {
      Action { implicit request: Request[AnyContent] =>
        log.info(
          s"Received home page request with minimum positivity " +
            s"$positivity from client ${request.remoteAddress}"
        )

        val categoryTables = LinkedHashMap[String, CategoryAnalysisPageData]()
        for ((categoryId, dbMetadata) <- DbConstants.categoryToDbMetadata) {
          log.info(
            s"Using db metadata ${dbMetadata.toString()} for category $categoryId."
          )
          dal.getCategoryAnalysisPage(cache, dbMetadata, positivity, 8) match {
            case Success(data) =>
              log.info(
                s"Parsing ${data.analysisRows.size} analysis row(s) and ${data.sourceFeeds.size} feed(s) into HTML template."
              )
              categoryTables(categoryId) = data

            case Failure(exc) =>
              log.error(s"Unable to get $categoryId page for home with exception $exc")
          }
        }
        Ok(
          views.html.home(
            categoryTables,
            positivity
          )
        ).as("text/html")
      }
    }

  def category(categoryId: String, positivity: Int) =
    cached.status(_ => "category" + categoryId + s"$positivity", status = 200, pageCacheTimeSec) {
      Action { implicit request: Request[AnyContent] =>
        log.info(
          s"Received request for category ${categoryId} page and minimum positivity " +
            s"$positivity from client ${request.remoteAddress}"
        )
        DbConstants.categoryToDbMetadata get categoryId match {
          case Some(dbMetadata) => {
            log.info(
              s"Using db metadata ${dbMetadata.toString()} for category $categoryId."
            )
            dal.getCategoryAnalysisPage(cache, dbMetadata, positivity) match {
              case Success(data) =>
                log.info(
                  s"Parsing ${data.analysisRows.size} analysis row(s) and ${data.sourceFeeds.size} feed(s) into HTML template."
                )
                Ok(
                  views.html.analysisPage(
                    data.analysisRows,
                    data.sourceFeeds,
                    data.lastUpdated,
                    categoryId,
                    positivity
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
      status = 200,
      pageCacheTimeSec
    ) {
      Action { implicit request: Request[AnyContent] =>
        log.info(
          s"Serving request from ${request.remoteAddress} for $categoryId entity " +
            s"${entityName} and type ${entityType} and sentiment ${sentiment}."
        )
        DbConstants.categoryToDbMetadata get categoryId match {
          case Some(dbMetadata) =>
            dal.getTextsPage(dbMetadata, entityName, entityType, sentiment) match {
              case Success(pageData) =>
                val books = Books.getBooks(entityType, sentiment)
                val entityTypeDescription =
                  EntityTypeDescriptions.descriptions getOrElse (entityType, entityType)
                Ok(
                  views.html.textsPage(
                    pageData.rows,
                    entityName,
                    entityType,
                    sentiment,
                    books,
                    entityTypeDescription
                  )
                ).as("text/html")

              case Failure(exc) =>
                log.error(s"Failed to get texts page with exception $exc")
                errResp
            }

          case _ => BadRequest(s"$categoryId is not a valid category ID.")
        }
      }
    }
}
