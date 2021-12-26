package fyi.newssnips.core

// import play.api._
// import scala.util.{Failure, Success, Try}
// import javax.inject._
import fyi.newssnips.models._
// import play.api.Logger
// import fyi.newssnips.shared.CategoryDbMetadata
// import scala.concurrent.{Future, blocking}
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent._
// import scala.concurrent.duration._
// import configuration.AppConfig
// import fyi.newssnips.datastore.Cache
// import play.api.libs.json._
// import fyi.newssnips.webapp.core._

case class CategoryAnalysisPageData(
    analysisRows: Array[AnalysisRow],
    sourceFeeds: Array[FeedRow],
    lastUpdated: String
)

case class TextsPageRow(
    text: String,
    url: String
)
case class EntityTextsPageData(
    rows: Array[TextsPageRow]
)

// @Singleton
// class PageDataFetcher() {
//   private val log: Logger = Logger("app." + this.getClass().toString())
//   lazy val db             = Postgres

//   // redis + db read timeout for dataframes
//   private val dataStoreWaitTime = if (AppConfig.settings.inProd) { 5.second }
//   else 15.seconds

//   implicit val feedRowFormat     = Json.format[FeedRow]
//   implicit val analysisRowFormat = Json.format[AnalysisRow]
//   implicit val pdRowFormat       = Json.format[CategoryAnalysisPageData]

//   /** Gets the category's page data from the DB by colelcting the necessary DFs
//     */
//   private def getCategoryPageDataDb(
//       categoryMetadata: CategoryDbMetadata
//   ): Try[CategoryAnalysisPageData] =
//     Try {
//       log.info(s"Getting analysis page data for category ${categoryMetadata.toString}")
//       val combined =
//         for {
//           apr <- Future { blocking { db.getDataframe(categoryMetadata.analysisTableName) } }
//           fdf <- Future { blocking { db.getDataframe(categoryMetadata.sourceFeedsTableName) } }
//           upd <- Future { blocking { db.getKV(categoryMetadata.lastUpdateKey) } }
//         } yield (apr, fdf, upd)

//       val (analysisDf: Try[DataFrame], feedsDf: Try[DataFrame], lastUpdated: Try[String]) =
//         Await.result(combined, dataStoreWaitTime)

//       log.info(s"Fetched all tables for page ${categoryMetadata.name}.")

//       (analysisDf, feedsDf, lastUpdated) match {
//         case (Success(analysisDf), Success(feedsDf), Success(lastUpdated)) =>
//           analysisDf.persist(StorageLevel.MEMORY_ONLY) // FIXME: needed for null array bug.
//           feedsDf.persist(StorageLevel.MEMORY_ONLY)

//           log.info(s"Persisted all tables for page ${categoryMetadata.name}.")

//           val pd = CategoryAnalysisPageData(
//             analysisRows = analysisDf.as[AnalysisRow].sort($"totalNumTexts".desc).collect(),
//             sourceFeeds = feedsDf.as[FeedRow].collect(),
//             lastUpdated = lastUpdated
//           )
//           log.info(s"Collected all tables for page ${categoryMetadata.name}.")
//           Future {
//             blocking {
//               analysisDf.unpersist()
//               feedsDf.unpersist()
//             }
//           }
//           pd
//         case (_, _, Failure(exc)) =>
//           throw new RuntimeException(
//             s"Unable to fetch analysis page last updated with exception $exc"
//           )

//         case _ =>
//           throw new RuntimeException(
/* s"Unable to fetch analysis page data. ${analysisDf.isFailure} ${feedsDf.isFailure}
 * ${lastUpdated.isFailure}" */
//           )
//       }
//     }

//   def getCategoryAnalysisPage(
//       cache: Cache,
//       categoryMetadata: CategoryDbMetadata
//   ): Try[CategoryAnalysisPageData] =
//     Try {
//       val cacheKey = categoryMetadata.name + ".page.data"
//       cache.get(cacheKey) match {
//         case Success(cachedRaw) =>
//           log.info(s"Cache hit for ${categoryMetadata.name} page data.")
//           Json.parse(cachedRaw).as[CategoryAnalysisPageData]
//         case _ =>
//           log.info(s"Page data cache miss for ${categoryMetadata.name}.")

//           getCategoryPageDataDb(categoryMetadata) match {
//             case Success(pd) =>
//               Future { blocking { cache.set(cacheKey, Json.toJson(pd).toString()) } }
//               pd
//             case Failure(e) =>
//               throw new RuntimeException(s"Failed to get category page data with error $e")
//           }
//       }
//     }

//   def getTextsPage(
//       categoryMetadata: CategoryDbMetadata,
//       entityName: String,
//       entityType: String,
//       sentiment: String
//   ): Try[EntityTextsPageData] =
//     Try {
//       val logIdentifier =
//         Seq(entityType, entityName, sentiment).mkString(
//           " - "
//         ) + categoryMetadata.toString()
//       log.info(
//         s"Getting texts page data for ${logIdentifier}."
//       )

//       val combined =
//         for {
//           apr   <- Future { blocking { db.getDataframe(categoryMetadata.analysisTableName) } }
//           urldf <- Future { blocking { db.getDataframe(categoryMetadata.articleUrlsTableName) } }
//           tdf   <- Future { blocking { db.getDataframe(categoryMetadata.textsTableName) } }
//         } yield (apr, urldf, tdf)

//       val (analysisDf: Try[DataFrame], urlsDf: Try[DataFrame], textsDf: Try[DataFrame]) =
//         Await.result(combined, dataStoreWaitTime)

//       (analysisDf, urlsDf, textsDf) match {
//         case (Success(analysisDf), Success(urlsDf), Success(textsDf)) =>
//           log.info(
//             s"Combining tables for texts page data extraction for ${logIdentifier}"
//           )

//           val filterDf = analysisDf
//             .select("entityName", "entityType", "negativeTextIds", "positiveTextIds")
//             .filter($"entityName" === entityName && $"entityType" === entityType)
//           if (filterDf.isEmpty) {
//             log.info(
//               s"No analysis rows found for entity ${logIdentifier}."
//             )
//             EntityTextsPageData(rows = Array.empty)
//           } else {

//             val idCol = sentiment match {
//               case "positive" => explode($"positiveTextIds").as("text_id")
//               case "negative" => explode($"negativeTextIds").as("text_id")
/* case _ => throw new RuntimeException(s"$sentiment is not a valid sentiment.") */
//             }
//             filterDf.persist(StorageLevel.MEMORY_ONLY)
//             urlsDf.persist(StorageLevel.MEMORY_ONLY)

//             val extractedDf = filterDf
//               .select(idCol)
//               .join(textsDf, Seq("text_id"), "inner")
//               .join(urlsDf, Seq("link_id"), "inner")
//               .dropDuplicates("text_id")
//               .select($"text", $"url")

//             log.info(
//               s"Collecting text and URLs for ${logIdentifier}."
//             )
//             // extractedDf.show()
//             val r = EntityTextsPageData(
//               rows = extractedDf
//                 .as[TextsPageRow]
//                 .collect()
//                 .map(r =>
//                   if (r.url.isEmpty()) // FIXME: when the source link was not extracted.
/* TextsPageRow(text = r.text, url = s"https://www.google.com/search?q=${r.text}") */
//                   else r
//                 )
//             )
//             log.info(
//               s"Collected text and URLs for ${logIdentifier}."
//             )
//             Future {
//               blocking {
//                 filterDf.unpersist()
//                 urlsDf.unpersist()
//               }
//             }
//             r
//           }
//         case _ =>
//           throw new RuntimeException(
//             s"Unable to fetch texts page data. ${urlsDf.isFailure} " +
//               s"${analysisDf.isFailure} ${textsDf.isFailure}."
//           )
//       }
//     }
// }
