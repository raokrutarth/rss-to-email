package fyi.newssnips.core

import com.sendgrid
import com.sendgrid._
import scala.util._
import fyi.newssnips.webapp.config.AppConfig
import sendgrid.helpers.mail.objects._
import sendgrid.helpers.mail._
import com.typesafe.scalalogging.Logger
import play.twirl.api.Html

object Mailer {
  private val log         = Logger("app." + this.getClass().toString())
  private val sg          = new SendGrid(AppConfig.settings.comms.sendgridApiKey)
  private val defaultFrom = new Email("no-reply@newssnips.fyi", "NewsSnips.fyi")

  def sendMail(to: Seq[String], subject: String, htmlContent: Html) = Try {
    val mail = new Mail()
    mail.setFrom(defaultFrom)

    for (r <- to) {
      // new personalization per target
      val p = new Personalization()
      p.addTo(new Email(r))
      mail.addPersonalization(p)
    }

    val content = new Content()
    content.setType("text/html")
    content.setValue(htmlContent.body)
    mail.addContent(content)

    mail.setSubject(subject)
    mail.addCategory("news")

    val request = new Request()

    request.setMethod(Method.POST)
    request.setEndpoint("mail/send")
    request.setBody(mail.build())
    val response = sg.api(request)
    log.info(
      s"Sent mail with status code: ${response.getStatusCode()}, response: ${response.getBody()}" +
        s" and headers ${response.getHeaders()}"
    )
  }
}
