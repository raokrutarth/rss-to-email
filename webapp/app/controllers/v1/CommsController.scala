package controllers.v1

import play.api._
import play.api.mvc._
import javax.inject._
import com.typesafe.scalalogging.Logger
import play.twirl.api.Html
import fyi.newssnips.datastore.Cache
import play.api.data.Form
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import fyi.newssnips.webapp.models._
import java.util.UUID
import fyi.newssnips.core.Mailer
import fyi.newssnips.webapp.config.AppConfig
import play.api.libs.json._
import scala.concurrent.{Future, blocking}
import fyi.newssnips.webapp.core.dal._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CommsController @Inject() (
    val cc: ControllerComponents,
    cache: Cache,
    dal: NewsletterDal
) extends AbstractController(cc)
    with play.api.i18n.I18nSupport {
  // https://pedrorijo.com/blog/play-forms/
  // TODO see compliance and features from newsletter
  // platforms
  // TODO security: give user a secret to make sure unsubscribe and
  // frequency change are controlled. OR send confirmation email.

  private val log: Logger = Logger("app." + this.getClass().toString())
  val errorResp = InternalServerError(
    views.html.siteTemplate("Error")(
      Html(
        """
          <div class='alert alert-danger col-md-6 offset-md-3'>
          <h3>An unknown server error occurred. Please try again later.</h3></div>
        """
      )
    )()
  ).as("text/html")

  private val verificationKeyPrefix = "webapp.newsletter.email.verify."
  implicit val signupDataFormat     = Json.format[SignupData]

  private val signupForm = Form(
    mapping(
      "email"     -> email,
      "frequency" -> nonEmptyText
    )(SignupData.apply)(SignupData.unapply)
  )
  private val submitUrl = routes.CommsController.newsletterSignupPost()

  def newsletterSignupGet(email: Option[String]) = Action { implicit request: Request[AnyContent] =>
    // prefill email if one is passed via query params
    val form = email match {
      case Some(v) => signupForm.fill(SignupData(v, Frequency.Weekly.toString()))
      case _       => signupForm
    }

    Ok(
      views.html.newsletter(
        form,
        submitUrl
      )
    ).as("text/html")
  }

  def newsletterSignupPost() = Action { implicit request: Request[AnyContent] =>
    val errorFunction = { formWithErrors: Form[SignupData] =>
      val errors = formWithErrors.errors.mkString(", ")
      log.info(s"Newsletter signup/update form validation failed with errors ${errors}.")
      // show the user the form again, with the errors highlighted.
      BadRequest(views.html.newsletter(formWithErrors, submitUrl))
    }

    val successFunction = { data: SignupData =>
      val sd = SignupData(
        data.email,
        data.frequency
      )
      log.info(s"Received validated newsletter signup/update request for ${sd.toString()}")

      // TODO update, already subscribed or full
      /** if email already present, update if new email and count in db allows, create redis
        * verification key else show paywall.
        */
      var successMessage = s"Verification email sent to ${sd.email}."
      var emailSubject   = "Welcome to the NewsSnips.fyi Newsletter"
      var isSignup       = true
      val exists         = dal.subscriberExists(sd.email)
      if (exists.isSuccess && exists.get) {
        successMessage =
          s"New verification email sent to ${sd.email}. Please click to save changes."
        emailSubject = "NewsSnips subscription update confirmation"
        isSignup = false
      }

      val token = UUID.randomUUID().toString()

      cache.set(verificationKeyPrefix + token, Json.toJson(sd).toString(), exSec = 86400) match {
        case false =>
          log.error(s"failed to set verification token for ${sd.email}.")
          errorResp
        case _ =>
          Mailer.sendMail(
            Seq(sd.email),
            emailSubject,
            views.html
              .emailTemplate(
                views.html.newsletterSignupEmail(
                  s"${AppConfig.settings.hostName}/v1/newsletter/verify?token=${token}",
                  emailSubject,
                  isSignup
                )
              )
          )
          Redirect(routes.CommsController.newsletterSignupGet(None))
            .flashing("success" -> successMessage)
      }
    }

    val formValidationResult: Form[SignupData] = signupForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def newsletterVerifyGet(token: String) = Action {
    cache.get(verificationKeyPrefix + token) match {
      case Some(sdJson) =>
        val sd = Json.parse(sdJson).as[SignupData]

        // TODO add to db
        if (dal.upsertSubscriber(sd).isSuccess) {
          log.info(
            s"${sd.email} sucessfully added to mail digest subscribers list for a ${sd.frequency} subscription."
          )
          // early manual delete
          Future {
            blocking {
              cache.delete(verificationKeyPrefix + token)
            }
          }
        }
        val currCount = dal.getSubscriberCount()
        if (currCount.isSuccess && currCount.get >= 100) {
          Ok(
            views.html.siteTemplate("Verified")(
              Html(
                s"""
                <div class='alert alert-warning col-md-6 offset-md-3'>
                  <h3>
                    ${sd.email} verified but we have already reach maximum capacity 
                    for free subscribers. You're on the list and we'll contact you shortly
                    when more capacity opens up or a paid plan is available.
                  </h3>
                </div>
              """
              )
            )()
          ).as("text/html")
        } else {
          Ok(
            views.html.siteTemplate("Verified")(
              Html(
                s"""
                <div class='alert alert-success col-md-6 offset-md-3'>
                  <h3>${sd.email} verified for a ${sd.frequency} digest on NewsSnips.</h3>
                </div>
              """
              )
            )()
          ).as("text/html")
        }
      case _ =>
        log.error(s"Newsletter subscription verification failed for token $token.")
        BadRequest("Newsletter signup verification failed. Please try again later.")
    }

  }
}
