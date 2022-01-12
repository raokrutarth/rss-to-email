package fyi.newssnips.webapp.newsletter.operations

import fyi.newssnips.webapp.models.SignupData
import com.typesafe.scalalogging.Logger

object NewsletterOperationsResponse extends Enumeration {
  type NewsletterOperationsResponse = Value
  val Subscribed, FormErrors, PreferencesUpdated, Unsubscribed, MaxFreeCapacity,
      InvalidVerificationToken, DoesNotExist, NewslettersPublished = Value
}
import NewsletterOperationsResponse._

object NewsletterOperations {

  Logger("app." + this.getClass().toString())

  def handleVerifiedAction(sd: SignupData): NewsletterOperationsResponse = {
    // add new subscriber
    // update existing subscriber
    // remove subscriber
    Subscribed
  }

  def publishDueNewsletters() = {
    0
  }
}
