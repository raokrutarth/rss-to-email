package fyi.newssnips.webapp.datastore

import scala.util.{Failure, Success, Try}
import java.time.LocalDate
import java.io._
import org.apache.commons._
import org.apache.http._
import org.apache.http.client._
import org.apache.http.client.methods.{
  HttpDelete,
  HttpGet,
  HttpPatch,
  HttpPost,
  HttpPut,
  HttpRequestBase
}

import org.apache.spark.sql.types._
import org.apache.spark.sql.DataFrame

import org.apache.http.impl.client.DefaultHttpClient
import scala.concurrent.Future
import javax.inject._
import play.api.inject.ApplicationLifecycle
import configuration.AppConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import java.util.ArrayList
import org.apache.http.message.BasicNameValuePair
import org.apache.http.impl.client.HttpClientBuilder
import play.api.libs.json._
import org.apache.http.client.methods.HttpPut
import org.apache.http.util.EntityUtils
import org.apache.http.entity.StringEntity
import fyi.newssnips.models.{AnalysisRow, Feed, FeedContent, FeedURL}
import play.api.Logger

/* https://stargate.io/docs/stargate/1.0/developers-guide/document-using.html#_retrieving_a_document_using_a_where_clause */

// temporary json based cloud storage layer.
@Singleton
class DocumentStore @Inject() (lifecycle: ApplicationLifecycle) {

  // val logger: Logger = Logger(this.getClass())

  // // default userd ID used until login
  // // and session management is added
  // private val dummyUserID = "first@user.com"

  // private val httpClient = HttpClientBuilder.create().build()

  // private val keySpace = if (AppConfig.settings.inProd) "prod" else "dev"
  // private val apiPath =
  //   "/api/rest/v2/namespaces/" + keySpace + "/collections"

  // private val feedsCollection = "feeds"

  // // TODO use to keep track of user to feed mappings
  // private val userToFeedsCollections = "user-to-feeds"

  // implicit val analysisRowFormat = Json.format[AnalysisRow]
  // implicit val feedUrlFormat     = Json.format[FeedURL]
  // implicit val feedContentFormat = Json.format[FeedContent]
  // implicit val feedFormat        = Json.format[Feed]

  // private def addAuth(request: HttpRequestBase): Unit = {
  //   request.setHeader("X-Cassandra-Token", AppConfig.settings.database.appToken)
  //   request.setHeader("Content-Type", "application/json")
  //   request.setHeader("Accept", "application/json")
  // }

  // def init() = {
  //   logger.info(
  //     s"Starting data layer with namespace $keySpace and collection $feedsCollection"
  //   )
  // }

  // def getUser(): String = dummyUserID

  // /** Get the feeds for the given user. * */
  // def getFeeds(userID: String = dummyUserID): Try[Seq[Feed]] = {
  //   val url = AppConfig.settings.database.url + apiPath + s"/$feedsCollection"
  //   logger.info(s"Getting feed URLs for user $userID from DB API $url")
  //   val request = new HttpGet(url)
  //   addAuth(request)

  //   val response    = httpClient.execute(request)
  //   val status_code = response.getStatusLine().getStatusCode()
  //   val payload     = EntityUtils.toString(response.getEntity())

  //   status_code match {
  //     case 200 =>
  //       val content = Json.parse(payload)
  //       // the data field has a mapping from doc-id to feed
  //       val docIdToFeed = content("data").as[Map[String, Feed]]
  //       logger.info(s"Fetched ${docIdToFeed.size} feeds for user $userID")
  //       Success(docIdToFeed.values.toSeq)
  //   }
  // }

  // def getAnalysis(key: String): Try[Array[AnalysisRow]] = {
  //   val url = AppConfig.settings.database.url + apiPath + s"/analysisStore/$key"
  //   logger.info(s"Getting getting analysis from DB API $url")
  //   val request = new HttpGet(url)
  //   addAuth(request)

  //   val response    = httpClient.execute(request)
  //   val status_code = response.getStatusLine().getStatusCode()
  //   val payload     = EntityUtils.toString(response.getEntity())

  //   status_code match {
  //     case 200 =>
  //       val content = Json.parse(payload)
  //       // the data field has a mapping from doc-id to feed
  //       val analysisRows = content("data").as[Array[AnalysisRow]]
  //       logger.info(s"Fetched ${analysisRows.size} feeds")
  //       Success(analysisRows)
  //   }
  // }

  // def upsertAnalysis(key: String, rows: Array[AnalysisRow]): Try[String] = {
  //   val url =
  //     AppConfig.settings.database.url + apiPath + s"/analysisStore/$key"
  //   logger.info(
  //     s"Storing dataframe with key $key using DB API $url"
  //   )
  //   val request = new HttpPut(url)
  //   addAuth(request)
  //   val feedJson =
  //     request.setEntity(new StringEntity(Json.toJson(rows).toString()))

  //   val response    = httpClient.execute(request)
  //   val status_code = response.getStatusLine().getStatusCode()
  //   val message     = EntityUtils.toString(response.getEntity())
  //   status_code match {
  //     case 200 =>
  //       logger.info(s"Saved dataframe with message $message")
  //       val docId = Json.parse(message)("documentId")
  //       Success(docId.as[String])
  //     case _ =>
  //       logger.error(s"Failed to save dataframe with key $key")
  //       throw new HttpException(
  //         s"Invalid status code $status_code and message $message during dataframe upsert."
  //       )
  //   }
  // }

  // /** Add or update an existing feed
  //   *
  //   * @param feed
  //   * @param userID
  //   * @return
  //   *   the document ID of the feed.
  //   */
  // def upsertFeed(feed: Feed, userID: String = dummyUserID): Try[String] = {
  //   val docId = feed.url.digest()
  //   val url =
  //     AppConfig.settings.database.url + apiPath + s"/$feedsCollection/$docId"
  //   logger.info(s"Storing feed $feed with DB API $url for user $userID")
  //   val request = new HttpPut(url)
  //   addAuth(request)
  //   val feedJson =
  //     request.setEntity(new StringEntity(Json.toJson(feed).toString()))

  //   val response    = httpClient.execute(request)
  //   val status_code = response.getStatusLine().getStatusCode()
  //   val message     = EntityUtils.toString(response.getEntity())
  //   status_code match {
  //     case 200 =>
  //       logger.info(s"Saved feed with message $message")
  //       val docId = Json.parse(message)("documentId")
  //       Success(docId.as[String])
  //   }
  // }

  // def deleteFeed(feedUrl: FeedURL): Try[Boolean] = {
  //   val docId = feedUrl.digest()
  //   val url =
  //     AppConfig.settings.database.url + apiPath + s"/$feedsCollection/$docId"
  //   logger.info(s"Deleting feed $feedUrl with DB API $url")
  //   val request = new HttpDelete(url)
  //   addAuth(request)

  //   val response    = httpClient.execute(request)
  //   val status_code = response.getStatusLine().getStatusCode()
  //   status_code match {
  //     case 204 =>
  //       // FIXME same status code even if the document does not exist
  //       logger.info(s"Deleted feed ${feedUrl}")
  //       Success(true)
  //     case _ =>
  //       logger.error(
  //         s"Unable to delete feed $feedUrl with status code $status_code."
  //       )
  //       Success(false)
  //   }
  // }

  // lifecycle.addStopHook { () =>
  //   Future.successful(logger.info("Application db end hook called"))
  // }
}
