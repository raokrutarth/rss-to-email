package fyi.newssnips.core

import play.api._
import scala.util.{Failure, Success, Try}
import javax.inject._
import fyi.newssnips.models.AnalysisRow
import play.api.Logger
import fyi.newssnips.datastore.DatastaxCassandra
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

import DatastaxCassandra.spark.implicits._
import fyi.newssnips.webapp.datastore.Cache
import fyi.newssnips.shared.CategoryDbMetadata
import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import configuration.AppConfig
import org.apache.spark.storage.StorageLevel

case class CategoryAnalysisPageData(
    analysisRows: Array[AnalysisRow],
    sourceFeeds: Array[String],
    lastUpdated: String
)

case class TextsPageRow(
    text: String,
    url: String
)
case class EntityTextsPageData(
    rows: Array[TextsPageRow]
)

@Singleton
class PageDataFetcher(
    // spark: SparkSession,
    cache: Cache
) {
  private val log: Logger = Logger("app." + this.getClass().toString())
  val db                  = DatastaxCassandra

  // redis + db read timeout for dataframes
  private val dataStoreWaitTime = if (AppConfig.settings.inProd) { 5.second }
  else 15.seconds

  /* fetch tables with caching */
  private def getCachedDf(tableName: String): Try[DataFrame] = Try {
    cache.getDf(tableName) match {
      case Success(df) =>
        df.show()
        df // cache hit

      // cache miss. go to db and set cache.
      case Failure(exc) =>
        log.error(s"Unable to load table $tableName from cache with exception $exc")

        db.getDataframe(tableName) match {
          case Success(df) =>
            // df.persist() // need for the write to work.

            Future {
              blocking {
                cache.putDf(tableName, df) match {
                  case Failure(exc) =>
                    log.error(
                      s"Failed to save table $tableName in cache after db fetch with exception $exc"
                    )
                  case _ => log.info(s"Saved table $tableName in cache after db fetch.")
                }

              }
            }

            df
          case _ => throw new RuntimeException(s"Unable to get table $tableName from db.")
        }
    }
  }

  def getCategoryAnalysisPage(categoryMetadata: CategoryDbMetadata): Try[CategoryAnalysisPageData] =
    Try {
      log.info(s"Getting analysis page data for category ${categoryMetadata.toString}")

      val combined =
        for {
          apr <- Future { blocking { getCachedDf(categoryMetadata.analysisTableName) } }
          fdf <- Future { blocking { getCachedDf(categoryMetadata.sourceFeedsTableName) } }
          upd <- Future { blocking { db.getKV(categoryMetadata.lastUpdateKey) } }
        } yield (apr, fdf, upd)

      val (analysisDf: Try[DataFrame], feedsDf: Try[DataFrame], lastUpdated: Try[String]) =
        Await.result(combined, dataStoreWaitTime)

      (analysisDf, feedsDf, lastUpdated) match {
        case (Success(analysisDf), Success(feedsDf), Success(lastUpdated)) =>
          analysisDf.persist(StorageLevel.MEMORY_ONLY)
          feedsDf.persist(StorageLevel.MEMORY_ONLY)

          val pd = CategoryAnalysisPageData(
            analysisRows = analysisDf.as[AnalysisRow].sort($"totalNumTexts".desc).collect(),
            sourceFeeds = feedsDf.select($"url").map(f => f.getString(0)).collect.toArray,
            lastUpdated = lastUpdated
          )
          Future {
            blocking {
              analysisDf.unpersist()
              feedsDf.unpersist()
            }
          }
          pd
        case (_, _, Failure(exc)) =>
          throw new RuntimeException(
            s"Unable to fetch analysis page last updated with exception $exc"
          )

        case _ =>
          throw new RuntimeException(
            s"Unable to fetch analysis page data. ${analysisDf.isFailure} ${feedsDf.isFailure} ${lastUpdated.isFailure}"
          )
      }
    }

  def getTextsPage(
      categoryMetadata: CategoryDbMetadata,
      entityName: String,
      entityType: String,
      sentiment: String
  ): Try[EntityTextsPageData] =
    Try {
      log.info(
        s"Getting texts page data for entity ${entityName} and " +
          s"type ${entityType} and sentiment ${sentiment} with ${categoryMetadata.toString()}."
      )

      val combined =
        for {
          apr   <- Future { blocking { getCachedDf(categoryMetadata.analysisTableName) } }
          urldf <- Future { blocking { getCachedDf(categoryMetadata.articleUrlsTableName) } }
          tdf   <- Future { blocking { getCachedDf(categoryMetadata.textsTableName) } }
        } yield (apr, urldf, tdf)

      val (analysisDf: Try[DataFrame], urlsDf: Try[DataFrame], textsDf: Try[DataFrame]) =
        Await.result(combined, dataStoreWaitTime)

      (analysisDf, urlsDf, textsDf) match {
        case (Success(analysisDf), Success(urlsDf), Success(textsDf)) =>
          val filterDf = analysisDf
            .filter($"entityName" === entityName && $"entityType" === entityType)
          if (filterDf.isEmpty) {
            log.info(
              s"No analysis rows found for entity ${entityName} and " +
                s"type ${entityType} and sentiment ${sentiment}"
            )
            EntityTextsPageData(rows = Array.empty)
          } else {

            val idCol = sentiment match {
              case "positive" => explode($"positiveTextIds").as("text_id")
              case "negative" => explode($"negativeTextIds").as("text_id")
              case _          => throw new RuntimeException(s"$sentiment is not a valid sentiment.")
            }
            filterDf.persist(StorageLevel.MEMORY_ONLY)
            urlsDf.persist(StorageLevel.MEMORY_ONLY)

            val extractedDf = filterDf
              .select(idCol)
              .join(textsDf, Seq("text_id"), "inner")
              .join(urlsDf, Seq("link_id"), "inner")
              .dropDuplicates("text_id")
              .select($"text", $"url")

            log.info(
              s"Extracted text and URLs for entity ${entityName} and " +
                s"type ${entityType} and sentiment ${sentiment}"
            )
            extractedDf.show()
            val r = EntityTextsPageData(
              rows = extractedDf
                .as[TextsPageRow]
                .collect()
                .map(r =>
                  if (r.url.isEmpty()) // FIXME: when the source link was not extracted.
                    TextsPageRow(text = r.text, url = s"https://www.google.com/search?q=${r.text}")
                  else r
                )
            )
            Future {
              blocking {
                filterDf.unpersist()
                urlsDf.unpersist()
              }
            }
            r
          }
        case _ =>
          throw new RuntimeException(
            s"Unable to fetch texts page data. ${urlsDf.isFailure} " +
              s"${analysisDf.isFailure} ${textsDf.isFailure}."
          )
      }
    }
}
