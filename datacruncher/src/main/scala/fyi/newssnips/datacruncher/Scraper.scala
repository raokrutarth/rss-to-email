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

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

object Scraper {
  private val httpClient =
    HttpClientBuilder
      .create()
      .setDefaultRequestConfig(AppConfig.settings.httpClientConfig)
      .build()

  // https://github.com/ruippeixotog/scala-scraper#quick-start
  private val htmlParser = JsoupBrowser()

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
    request.setHeader(
      "Accept",
      "application/rss+xml, application/xml, application/atom+xml, text/xml" // application/rdf+xml
    )
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

  private def extractGNewsContent(xml: Elem): Seq[FeedContent] = {
    var secondaryRaw = Seq[String]()

    val primaryContent = for {
      xmlItems <- (xml \\ "item")
      title          = (xmlItems \\ "title").text
      descriptionRaw = (xmlItems \ "description").text
      link           = (xmlItems \\ "link").text
    } yield {
      // content has nested html with list
      // containing links and headlines to related
      // articles.
      secondaryRaw = secondaryRaw :+ descriptionRaw
      FeedContent(
        link,
        title,
        "",
        false
      )
    }
    var secondaryContent = Seq[FeedContent]()
    for (raw <- secondaryRaw) {
      try {
        val doc    = htmlParser.parseString(raw)
        val titles = (doc >> texts("a")).dropRight(1)
        val links  = (doc >> elementList("a") >> attr("href")("a"))

        for (tl <- (titles zip links)) yield {
          secondaryContent =
            secondaryContent :+ FeedContent(tl._1, tl._2, "", false)
        }
      } catch {
        case e: Exception =>
          log.error(
            s"Unable to extract secondary content from gnews description with error $e"
          )
      }
    }

    log.info(
      s"Extracted ${secondaryContent.size} secondary content items from gnews feed."
    )
    primaryContent ++ secondaryContent
  }

  private def extractRedditContent(
      xml: Elem,
      disableContent: Boolean
  ): Seq[FeedContent] = {
    for {
      xmlItem <- (xml \\ "entry")
      title = (xmlItem \\ "title").text
      // TODO fix by parsing the html content
      // with https://github.com/ruippeixotog/scala-scraper
      description = if (disableContent) "" else (xmlItem \\ "content").text
      link        = (xmlItem \\ "link" \ "@href").text
    } yield FeedContent(
      link,
      title,
      description,
      false
    )
  }

  private def extractBasicContent(
      xml: Elem,
      disableContent: Boolean
  ): Seq[FeedContent] = {
    for {
      xmlItem <- (xml \\ "item")
      title       = (xmlItem \\ "title").text
      description = if (disableContent) "" else (xmlItem \ "description").text
      link        = (xmlItem \\ "link").text
    } yield FeedContent(
      link,
      title,
      description,
      false
    )
  }

  def getAndParseFeed(
      url: FeedURL,
      disableContent: Boolean =
        true // don't read the description of each entry in xml
  ): Option[Feed] = {

    Scraper.getXML(url.value) match {
      case Success(xml) =>
        val feedTitle = (xml \\ "title")(0).text // .as[Option[String]]
        log.info(
          f"Extracting contents from feed ${url.value} with title $feedTitle."
        )

        val contents = if (url.value.contains("reddit.com")) {
          extractRedditContent(xml, disableContent)
        } else if (url.value.contains("news.google.com")) {
          extractGNewsContent(xml)
        } else {
          extractBasicContent(xml, disableContent)
        }
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
