package fyi.newssnips.datacruncher.core

import fyi.newssnips.models.{Feed, FeedContent, FeedURL}
import scala.xml.{Elem, XML}
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import configuration.AppConfig
import scala.util.{Failure, Success, Try}
import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.DateTimeUtils
import play.api.libs.json._

object Scraper {
  private val httpClient =
    HttpClientBuilder
      .create()
      .setDefaultRequestConfig(AppConfig.settings.httpClientConfig)
      .build()

  private val log: Logger = Logger("app." + this.getClass().toString())

  private def getXML(url: String): Try[Elem] = Try {
    log.info(s"Fetching XML from feed $url")
    val request = new HttpGet(
      url.strip()
    )
    request.setHeader(
      "user-agent",
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.2 Safari/605.1.15"
    )
    request.setHeader("Accept", "application/rss")
    val response    = httpClient.execute(request)
    val status_code = response.getStatusLine().getStatusCode()

    status_code match {
      case 200 =>
        XML.loadString(EntityUtils.toString(response.getEntity()))
      case _ =>
        log.error(
          s"Unable to fetch content from $url with status code $status_code"
        )
        throw new IllegalArgumentException(
          s"Invalid response status code $status_code"
        )
    }
  }

  def getApiFeed(categoryId: String = "general"): Option[Feed] = {
    // https://newsapi.org/docs/endpoints/top-headlines
    // TODO need to better pick scraper utility.
    val request = new HttpGet(
      s"https://newsapi.org/v2/top-headlines?country=us&category=${categoryId}&pageSize=100"
    )
    request.setHeader(
      "X-Api-Key",
      AppConfig.settings.newsApi.apiKey
    )
    val response    = httpClient.execute(request)
    val status_code = response.getStatusLine().getStatusCode()

    status_code match {
      case 200 =>
        val payload = Json.parse(EntityUtils.toString(response.getEntity()))
        log.info(s"first article: ${payload \\ "title"}")
        None
      case _ =>
        log.error(
          s"Unable to fetch content from news API with status code $status_code"
        )
        None
    }
  }

  def getAndParseFeed(url: FeedURL): Option[Feed] = {
    val isReddit: Boolean = url.value.contains("reddit.com")

    Scraper.getXML(url.value) match {
      case Success(xml) =>
        val feedTitle = (xml \\ "title")(0).text // .as[Option[String]]
        log.info(
          f"Extracting contents from feed ${url.value} with title $feedTitle."
        )

        val itemTag =
          if (isReddit) "entry" else "item"
        val contentTag =
          if (isReddit) "content" else "description"

        val contents = for {
          xmlItems <- (xml \\ itemTag)
          title       = (xmlItems \\ "title").text
          description = (xmlItems \\ contentTag).text
          link =
            if (!isReddit) (xmlItems \\ "link").text
            else (xmlItems \\ "link" \ "@href").text
        } yield FeedContent(
          link,
          title,
          description,
          false
        )
        log.info(s"Extracted ${contents.size} items form ${url.value}.")
        // TODO when lenght is 0, return failure
        if (contents.isEmpty) {
          None
        } else {
          Some(
            Feed(
              url = url,
              content = contents,
              title = feedTitle,
              lastScraped = DateTimeUtils.now()
            )
          )
        }

      case Failure(exc) =>
        log.error(
          s"Failed to get feed contents from ${url.value} because: $exc"
        )
        None
    }
  }
}
