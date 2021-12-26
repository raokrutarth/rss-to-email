package fyi.newssnips.datacruncher.core

import fyi.newssnips.datacruncher.models.{Feed, FeedURL, FeedContent}
import scala.xml.XML
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import configuration.AppConfig
import scala.util.{Try, Success, Failure}
import com.typesafe.scalalogging.Logger

object Scraper {
  private val httpClient =
    HttpClientBuilder
      .create()
      .setDefaultRequestConfig(AppConfig.settings.httpClientConfig)
      .build()

  val log: Logger = Logger(this.getClass())

  private def getXML(url: String): Try[String] = Try {
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
      case 200 => EntityUtils.toString(response.getEntity())
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
      case Success(payload) =>
        val xml = XML.loadString(payload)

        val feedTitle = (xml \\ "title")(0).text // .as[Option[String]]
        log.info(f"Found feed with title: $feedTitle")

        val itemTag = if (feedUrl.url.contains("reddit.com")) "entry" else "item"

        val contents = for {
          xmlItems <- (xml \\ itemTag)
          title       = (xmlItems \\ "title").text
          description = (xmlItems \\ "description").text
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
