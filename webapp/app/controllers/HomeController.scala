package controllers

import javax.inject.Inject
import javax.inject._
import play.api.mvc._
import play.api.cache.Cached
import com.typesafe.scalalogging.Logger
import fyi.newssnips.webapp.core._

@Singleton
class HomeController @Inject() (
    val controllerComponents: ControllerComponents,
    cached: Cached
) extends BaseController {

  val log = Logger("app." + this.getClass().toString())

  def index() = Action {
    Redirect("/about")
  }

  def about() = cached.status(_ => "aboutPage", status = 200) {
    Action {
      Ok(
        views.html.about()
      ).as("text/html")
    }
  }
}
