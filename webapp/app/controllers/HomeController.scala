package controllers

import javax.inject.Inject
import javax.inject._
import play.api.mvc._
import play.api.cache.Cached
import com.typesafe.scalalogging.Logger
import fyi.newssnips.core._
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future
import fyi.newssnips.datastore.Cache
import fyi.newssnips.webapp.core.dal.NewsletterDal

@Singleton
class HomeController @Inject() (
    val controllerComponents: ControllerComponents,
    lifecycle: ApplicationLifecycle,
    cached: Cached,
    auth: AdminAuth,
    cache: Cache,
    nsDal: NewsletterDal
) extends BaseController {

  val log = Logger("app." + this.getClass().toString())

  def index() = Action { _ =>
    // TODO take query args like ref and put in cassandra.
    Redirect("/v1/home")
  }

  def robotsTxt() = Action { _ =>
    // TODO add sitemap.
    Ok("""
User-agent: *
Allow: /
Disallow: */mentions/*

Sitemap: https://newssnips.fyi/sitemap.txt
    """).as("text/plain")
  }

  def sitemapTxt() = Action { _ =>
    // TODO add sitemap.
    Ok("""
https://newssnips.fyi/about
https://newssnips.fyi/v1/home
https://newssnips.fyi/v1/category/world
https://newssnips.fyi/v1/category/markets
https://newssnips.fyi/v1/category/entertainment
https://newssnips.fyi/v1/category/politics
    """).as("text/plain")
  }

  def adminDash(action: Option[String]) = auth { request =>
    log.info(s"Admin ${request.user} accessed the admin dashboard with action ${action}.")
    val pageVisits        = Seq("/some/a", "other/b", "rr/t")
    val numSubscribers    = nsDal.getSubscriberCount().getOrElse(0)
    val recentSubscribers = nsDal.getSubscribers().getOrElse(Array())
    val actionResponse: String = action match {
      case Some("flushCache") => {
        cache.flushCache()
        "cache flushed"
      }
      case Some(a) => {
        log.info(s"No known admin action to take for $a")
        s"$a is not a valid admin action"
      }
      case None => ""
    }
    Ok(
      views.html.admin(
        request.user.value,
        numSubscribers,
        recentSubscribers,
        pageVisits,
        actionResponse
      )
    ).as("text/html")
  }

  def about() = cached.status(_ => "aboutPage", status = 200) {
    Action {
      Ok(
        views.html.about()
      ).as("text/html")
    }
  }

  // all cleanup for the app occurs here
  lifecycle.addStopHook { () =>
    Future.successful {
      log.warn("Running custom application EOL hook.")
      cache.cleanup()
    }
  }
}
