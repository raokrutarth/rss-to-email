package controllers

import javax.inject.Inject
import javax.inject._
import play.api.mvc._
import play.api.cache.Cached
import fyi.newssnips.webapp.core.Postgres

@Singleton
class HomeController @Inject() (
    val controllerComponents: ControllerComponents,
    cached: Cached
) extends BaseController {

  def index() = Action {
    Postgres.queryCheck()
    // Redirect("/about")
    Ok("ok")
  }

  def about() = cached.status(_ => "aboutPage", status = 200) {
    Action {
      Ok(
        views.html.about()
      ).as("text/html")
    }
  }
}
