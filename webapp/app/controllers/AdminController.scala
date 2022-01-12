package controllers

import javax.inject.Inject
import javax.inject._
import play.api.mvc._
import com.typesafe.scalalogging.Logger
import fyi.newssnips.core._
import fyi.newssnips.datastore.Cache
import fyi.newssnips.webapp.core.dal.NewsletterDal

@Singleton
class AdminController @Inject() (
    val controllerComponents: ControllerComponents,
    auth: AdminAuth,
    cache: Cache,
    nsDal: NewsletterDal
) extends BaseController {

  val log = Logger("app." + this.getClass().toString())
  def adminDash(action: Option[String]) = auth { request =>
    log.info(s"Admin ${request.user} accessed the admin dashboard with action ${action}.")
    val pageVisits        = Seq("/wip/1", "/wip/2", "/wip/3")
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
}
