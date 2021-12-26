package fyi.newssnips.datacruncher.core

import fyi.newssnips.models.{Feed, FeedContent, FeedURL}
import scala.xml.XML
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import configuration.AppConfig
import scala.util.{Failure, Success, Try}
import com.typesafe.scalalogging.Logger

object Scraper {
  private val httpClient =
    HttpClientBuilder
      .create()
      .setDefaultRequestConfig(AppConfig.settings.httpClientConfig)
      .build()

  val log: Logger = Logger(this.getClass())

  private def getXML(url: String): Try[scala.xml.Elem] = Try {
    log.info(s"Fetching XML from feed $url")
    val request = new HttpGet(
      url.strip()
    )
    request.setHeader(
      "user-agent",
      "Mozilla/5.0"
    )
    request.setHeader("Accept", "application/rss+xml")
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

  def getContent(feedUrl: FeedURL): Option[Seq[FeedContent]] = {
    getXML(feedUrl.url) match {
      case Success(xml) =>
        val feedTitle = (xml \\ "title")(0).text // .as[Option[String]]
        log.info(f"Found feed with title: $feedTitle")

        val itemTag = if (feedUrl.url.contains("reddit.com")) "entry" else "item"
        val contentTag = if (feedUrl.url.contains("reddit.com")) "content" else "description"

        val contents = for {
          xmlItems <- (xml \\ itemTag)
          title       = (xmlItems \\ "title").text
          description = (xmlItems \\ contentTag).text
          link        = (xmlItems \\ "link").text
        } yield FeedContent(
          link,
          title,
          description,
          false
        )
        log.info(s"Extracted ${contents.size} items from $feedUrl")
        // TODO when lenght is 0, return failure
        Some(contents)

      case Failure(s) =>
        log.error(s"Failed to get XML content. Reason: $s")
        None
    }

  }
}
