package fyi.newssnips.core

import play.api._
import scala.util.{Failure, Success, Try}
import javax.inject._
import fyi.newssnips.models._
import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.CategoryDbMetadata
import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import fyi.newssnips.webapp.config.AppConfig
import fyi.newssnips.datastore.Cache
import play.api.libs.json._
import fyi.newssnips.webapp.core.dal._
import fyi.newssnips.webapp.core.db._

case class CategoryAnalysisPageData(
    analysisRows: Array[AnalysisRowUi],
    sourceFeeds: Array[FeedRow],
    lastUpdated: String
)
case class EntityTextsPageData(
    rows: Array[TextsPageRow]
)

@Singleton
class PageDataFetcher @Inject() (dal: PgAccess, db: Postgres)() {
  private val log: Logger = Logger("app." + this.getClass().toString())
  // redis + db read timeout for dataframes
  private val dataStoreWaitTime = if (AppConfig.settings.shared.inProd) { 5.second }
  else 15.seconds

  implicit val feedRowFormat     = Json.format[FeedRow]
  implicit val analysisRowFormat = Json.format[AnalysisRowUi]
  implicit val pdRowFormat       = Json.format[CategoryAnalysisPageData]

  /** Gets the category's page data from the DB by colelcting the necessary DFs
    */
  private def getCategoryPageDataDb(
      categoryMetadata: CategoryDbMetadata,
      minPositivity: Int,
      limit: Int,
      offset: Int
  ): Try[CategoryAnalysisPageData] =
    Try {
      log.info(s"Getting analysis page data for category ${categoryMetadata.toString}")

      val analysisTry    = dal.getAnalysisRows(categoryMetadata, minPositivity, limit, offset)
      val feedsTry       = dal.getFeedsRows(categoryMetadata)
      val lastUpdatedTry = db.getKv(categoryMetadata.lastUpdateKey)

      (analysisTry, feedsTry, lastUpdatedTry) match {
        case (Success(a), Success(f), Success(l)) =>
          CategoryAnalysisPageData(
            analysisRows = a,
            sourceFeeds = f,
            lastUpdated = l.get
          )
        case (Failure(e), _, _) =>
          log.error(s"analysis DB fetch exception: $e")
          throw e
        case _ =>
          throw new RuntimeException(
            "Failed to get analysis page data from db with" +
              s"failure status (${analysisTry.isFailure}, ${feedsTry.isFailure}, " +
              s"${lastUpdatedTry.isFailure})"
          )
      }

    }

  def getCategoryAnalysisPage(
      cache: Cache,
      categoryMetadata: CategoryDbMetadata,
      minPositivity: Int,
      limit: Int = 100,
      offset: Int = 0
  ): Try[CategoryAnalysisPageData] =
    Try {
      val cacheKey =
        categoryMetadata.name + ".page.data" + s".min.pos.$minPositivity.limit.$limit.offset.$offset"
      cache.get(cacheKey) match {
        case Some(cachedRaw) =>
          log.info(
            s"Cache hit for ${cacheKey}."
          )
          Json.parse(cachedRaw).as[CategoryAnalysisPageData]
        case _ =>
          log.info(s"Page data cache miss for ${cacheKey}.")

          val dbFetch = Future {
            blocking {
              getCategoryPageDataDb(categoryMetadata, minPositivity, limit, offset)
            }
          }

          Await.result(dbFetch, dataStoreWaitTime) match {
            case Success(pd) =>
              Future { blocking { cache.set(cacheKey, Json.toJson(pd).toString()) } }
              pd
            case Failure(e) =>
              throw new RuntimeException(s"Failed to get category page data with error $e")
          }
      }
    }

  def getTextsPage(
      categoryMetadata: CategoryDbMetadata,
      entityName: String,
      entityType: String,
      sentiment: String
  ): Try[EntityTextsPageData] =
    Try {
      dal.getTexts(
        categoryMetadata,
        entityName,
        entityType,
        sentiment
      ) match {
        case Success(rows) =>
          EntityTextsPageData(rows)
        case Failure(e) =>
          val id = Seq(entityType, entityName, sentiment).mkString(
            " - "
          ) + categoryMetadata.toString()
          log.error(s"Unable to get entity texts for $id with exception $e")
          throw new RuntimeException(s"Unable to get entity texts for $id")
      }
    }
}
