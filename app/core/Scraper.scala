package core

import models.{Feed, FeedURL, FeedContent}
import scala.xml.XML
import play.api.Logger
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import configuration.AppConfig
import scala.util.{Try, Success, Failure}

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
      // "http://rss.cnn.com/rss/money_latest.rss"
      // "http://www.chicagotribune.com/sports/rss2.0.xml"
    )
    val response = httpClient.execute(request)
    request.setHeader(
      "user-agent",
      "Mozilla/5.0"
    )
    // request.setHeader("Content-Type", "application/json")
    request.setHeader("Accept", "application/rss+xml")
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

  def getContent(feed: Feed): Option[Seq[FeedContent]] = {
    getXML(feed.url.url) match {
      case Success(payload) =>
        val xml = XML.loadString(payload)

        val contents = for {
          xmlItems <- (xml \\ "item")
          title = (xmlItems \\ "title").text
          description = (xmlItems \\ "description").text
          link = (xmlItems \\ "link").text
        } yield FeedContent(
          link,
          title,
          description,
          false
        )
        log.info(s"Extracted ${contents.size} items from $feed")
        log.info(s"${contents.head}")
        Some(contents)

      case Failure(s) =>
        log.error(s"Failed to get XML content. Reason: $s")
        None
    }

  }
}
