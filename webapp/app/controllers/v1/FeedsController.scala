// package controllers.v1

// import play.api._
// import play.api.mvc._
// import scala.util.{Failure, Success}
// import javax.inject.Inject
// import javax.inject._
// import play.api.Logger
// import fyi.newssnips.datastore.DatastaxCassandra
// import play.api.inject.ApplicationLifecycle
// import scala.concurrent.Future
// import fyi.newssnips.shared.DbConstants
// import fyi.newssnips.core.PageDataFetcher
// import play.api.cache.Cached
// import play.twirl.api.Html
// import fyi.newssnips.datastore.Cache
// import configuration.AppConfig
// import play.api.libs.json._
// import fyi.newssnips.core.CategoryAnalysisPageData
// import scala.util.Try
// import fyi.newssnips.models._
// import play.api.libs.json._
// // import fyi.newssnips.models.AnalysisRow

// @Singleton
// class FeedsController @Inject() (
//     val controllerComponents: ControllerComponents,
//     lifecycle: ApplicationLifecycle,
//     cached: Cached,
//     cache: Cache
// ) extends BaseController {

//   private val log: Logger = Logger("app." + this.getClass().toString())
//   private val dataFetcher = new PageDataFetcher()
//   private val errResp = InternalServerError(
//     views.html.siteTemplate("Error")(
//       Html(
//         """
//           <div class='alert alert-danger col-md-6 offset-md-3'>
//           <h3>An unknown server error occurred. Please try again later.</h3></div>
//         """
//       )
//     )
//   ).as("text/html")

//   val pageCacheTimeSec: Int = if (AppConfig.settings.inProd) 30 else 5

//   def home() = cached.status(_ => "homeAnalysisPage", status = 200, pageCacheTimeSec) {
//     Action { implicit request: Request[AnyContent] =>
//       val dbMetadata = DbConstants.categoryToDbMetadata("home")
//       log.info(
//         s"Received home page request from client ${request.remoteAddress}. " +
//           s"Using db metadata ${dbMetadata.toString()}"
//       )
//       dataFetcher.getCategoryAnalysisPage(cache, dbMetadata) match {
//         case Success(data) =>
//           log.info(
//             s"Parsing ${data.analysisRows.size} analysis row(s) and " +
//               s"${data.sourceFeeds.size} feed(s) into HTML template."
//           )
//           // TODO progress bar for sentiment scale
//           // https://www.w3schools.com/bootstrap/bootstrap_progressbars.asp

//           Ok(
//             views.html.analysisPage(
//               data.analysisRows,
//               data.sourceFeeds,
//               data.lastUpdated,
//               "home"
//             )
//           ).as("text/html")

//         case Failure(exc) =>
//           log.error(s"Unable to get home page with exception $exc")
//           errResp
//       }
//     }
//   }

//   def category(categoryId: String) =
//     cached.status(_ => "category" + categoryId, status = 200, pageCacheTimeSec) {
//       Action { implicit request: Request[AnyContent] =>
//         log.info(
//           s"Received category ${categoryId} page request from client ${request.remoteAddress}"
//         )
//         DbConstants.categoryToDbMetadata get categoryId match {
//           case Some(dbMetadata) => {
//             log.info(
//               s"Using db metadata ${dbMetadata.toString()} for category $categoryId."
//             )
//             dataFetcher.getCategoryAnalysisPage(cache, dbMetadata) match {
//               case Success(data) =>
//                 log.info(
/* s"Parsing ${data.analysisRows.size} analysis row(s) and ${data.sourceFeeds.size} feed(s) into
 * HTML template." */
//                 )
//                 Ok(
//                   views.html.analysisPage(
//                     data.analysisRows,
//                     data.sourceFeeds,
//                     data.lastUpdated,
//                     categoryId
//                   )
//                 ).as("text/html")

//               case Failure(exc) =>
//                 log.error(s"Unable to get $categoryId page with exception $exc")
//                 errResp
//             }
//           }
//           case _ => BadRequest(s"$categoryId is not a valid category.")
//         }
//       }
//     }

//   def mentions(categoryId: String, entityName: String, entityType: String, sentiment: String) =
//     cached.status(
//       _ => "mentions" + categoryId + entityName + entityType + sentiment,
//       status = 200,
//       pageCacheTimeSec
//     ) {
//       Action { implicit request: Request[AnyContent] =>
//         log.info(
//           s"Serving request from ${request.remoteAddress} for $categoryId entity " +
//             s"${entityName} and type ${entityType} and sentiment ${sentiment}."
//         )
//         DbConstants.categoryToDbMetadata get categoryId match {
//           case Some(dbMetadata) =>
//             dataFetcher.getTextsPage(dbMetadata, entityName, entityType, sentiment) match {
//               case Success(pageData) =>
//                 Ok(
//                   views.html.textsPage(pageData.rows, entityName, entityType, sentiment)
//                 ).as("text/html")

//               case Failure(exc) =>
//                 log.error(s"Failed to get texts page with exception $exc")
//                 errResp
//             }

//           case _ => BadRequest(s"$categoryId is not a valid category ID.")
//         }
//       }
//     }

//   lifecycle.addStopHook { () =>
//     Future.successful {
//       log.warn("Running feeds controller EOL hook.")
//       cache.cleanup()
//     }
//   }
// }
