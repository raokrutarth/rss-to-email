package controllers.v1

import play.api._
import play.api.mvc._
import scala.util.{Failure, Success}
import javax.inject.Inject
import javax.inject._
import com.typesafe.scalalogging.Logger
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future
import fyi.newssnips.shared.DbConstants
import fyi.newssnips.core.PageDataFetcher
import play.api.cache.Cached
import play.twirl.api.Html
import fyi.newssnips.datastore.Cache
import configuration.AppConfig
import play.api.libs.json._
import fyi.newssnips.core.CategoryAnalysisPageData
import scala.util.Try
import fyi.newssnips.models._
import play.api.libs.json._
import fyi.newssnips.webapp.core.Postgres
import java.util.UUID
import play.api.data.Form
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.mvc._

object Frequency extends Enumeration {
  val Daily, Weekly, Monthly = Value
}
case class SignupData(email: String, frequency: String)

@Singleton
class CommsController @Inject() (
    val cc: ControllerComponents,
    cache: Cache
) extends AbstractController(cc)
    with play.api.i18n.I18nSupport {
  // https://pedrorijo.com/blog/play-forms/

  private val log: Logger = Logger("app." + this.getClass().toString())
  private val errResp = InternalServerError(
    views.html.siteTemplate("Error")(
      Html(
        """
          <div class='alert alert-danger col-md-6 offset-md-3'>
          <h3>An unknown server error occurred. Please try again later.</h3></div>
        """
      )
    )
  ).as("text/html")

  private val signupForm = Form(
    mapping(
      "email"     -> email,
      "frequency" -> nonEmptyText
    )(SignupData.apply)(SignupData.unapply)
  )
  private val mainPostUrl = routes.CommsController.newsletterSignupPost()

  def newsletterSignupGet() = Action { implicit request: Request[AnyContent] =>
    Ok(
      views.html.newsletter(signupForm, mainPostUrl)
    ).as("text/html")
  }

  def newsletterSignupPost() = Action { implicit request: Request[AnyContent] =>
    val errorFunction = { formWithErrors: Form[SignupData] =>
      log.info("CAME INTO errorFunction")
      // this is the bad case, where the form had validation errors.
      // show the user the form again, with the errors highlighted.
      log.error(formWithErrors.errors.mkString(", "))
      BadRequest(views.html.newsletter(formWithErrors, mainPostUrl))
    }

    val successFunction = { data: SignupData =>
      log.info("CAME INTO successFunction")
      // this is the SUCCESS case, where the form was successfully parsed as a BlogPost
      val blogPost = SignupData(
        data.email,
        data.frequency
      )
      log.info(blogPost.toString)
      Redirect(routes.CommsController.newsletterSignupGet())
        .flashing("message" -> s"Verification email sent to ${data.email}.")
    }

    val formValidationResult: Form[SignupData] = signupForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def newsletterVerifyGet(token: String) = Action { implicit request: Request[AnyContent] =>
    cache.get(token) match {
      case Success(email) =>
        Ok(
          email + " verified"
        ).as("text/html")
      case _ =>
        BadRequest("newsletter signup verification failed.")
    }
  }
}
