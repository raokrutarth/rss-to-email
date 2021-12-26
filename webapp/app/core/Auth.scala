package fyi.newssnips.core

import java.util.Base64
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import scala.concurrent.ExecutionContext
import javax.inject.Inject
import java.security.MessageDigest
import configuration.AppConfig

case class User(value: String)     extends AnyVal
case class Password(value: String) extends AnyVal
case class Credentials(user: User, password: Password)

object AuthenticationHelpers {
  val adminUsername = AppConfig.settings.adminAuth.username.getBytes()
  val adminPwd      = AppConfig.settings.adminAuth.password.getBytes()

  private def parseAuthHeader(authHeader: String): Option[Credentials] = {
    authHeader.split("""\s""") match {
      case Array("Basic", userAndPass) ⇒
        new String(Base64.getDecoder.decode(userAndPass), "UTF-8").split(":") match {
          case Array(user, password) ⇒ Some(Credentials(User(user), Password(password)))
          case _                     ⇒ None
        }
      case _ ⇒ None
    }
  }

  private def validateUser(c: Credentials): Option[User] = {
    if (
      MessageDigest
        .isEqual(c.user.value.getBytes(), adminUsername) &
        MessageDigest.isEqual(c.password.value.getBytes(), adminPwd)
    )
      // populates the request.user parameter for the downstream action.
      Some(c.user)
    else
      None
  }

  def extractUser(rh: RequestHeader): Option[User] = {
    rh.headers
      .get("Authorization")
      .flatMap(AuthenticationHelpers.parseAuthHeader)
      .flatMap(AuthenticationHelpers.validateUser)
  }

  def onUnauthorized(rh: RequestHeader) = {
    Unauthorized(views.html.defaultpages.unauthorized()(rh))
      // need to set header for the browser to prompt for creds.
      .withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
  }
}

/** Usage:
  * https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Custom-action-builders
  */
class AdminAuth(parser: BodyParser[AnyContent])(implicit ec: ExecutionContext)
    extends AuthenticatedBuilder[User](
      // https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Authentication
      AuthenticationHelpers.extractUser,
      parser,
      AuthenticationHelpers.onUnauthorized
    ) {

  @Inject()
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent])
  }
}
