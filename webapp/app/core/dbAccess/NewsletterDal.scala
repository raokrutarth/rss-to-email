package fyi.newssnips.webapp.core.dal

import com.typesafe.scalalogging.Logger
import java.sql.ResultSet
import scala.util._
import java.sql.PreparedStatement
import fyi.newssnips.webapp.core.db.Postgres
import fyi.newssnips.webapp.models._
import play.api.libs.json.Json
import javax.inject._

@Singleton
class NewsletterDal @Inject() (db: Postgres) {
  val log                 = Logger("app." + this.getClass().toString())
  val subscriberTableName = "newsletter_subscribers"

  def getDueSubscribers(): Try[Array[SubscriberRow]] = {

    // TODO add pagination
    val q = s"""
        SELECT * FROM ${subscriberTableName} 
        WHERE 
        (
            frequency = '${Frequency.Monthly.toString}'
            AND last_digest_at < (timezone('utc'::text, now()) - interval '30 day')
        )
        OR
        (
            frequency = '${Frequency.Weekly.toString}'
            AND last_digest_at < (timezone('utc'::text, now()) - interval '7 day')
        )
    """
    val parser = (r: ResultSet) => {
      SubscriberRow(
        email = r.getString("email"),
        frequency = r.getString("frequency"),
        createdAt = Some(r.getTimestamp("created_at")),
        lastDigestAt = None,
        metadata = {
          val mJson = r.getString("metadata")
          if (mJson != null && mJson.nonEmpty)
            Some(Json.parse(mJson))
          else
            None
        }
      )
    }

    db.getRows[SubscriberRow](q, parser = parser)
  }
  def getSubscribers(limit: Int = 10, offset: Int = 0): Try[Array[SubscriberRow]] = {

    // TODO add pagination
    val q = s"""
        SELECT * FROM ${subscriberTableName} 
        ORDER BY created_at
        LIMIT ? OFFSET ?;
    """
    val parser = (r: ResultSet) => {
      SubscriberRow(
        email = r.getString("email"),
        frequency = r.getString("frequency"),
        createdAt = Some(r.getTimestamp("created_at")),
        lastDigestAt = None,
        metadata = {
          val mJson = r.getString("metadata")
          if (mJson != null && mJson.nonEmpty)
            Some(Json.parse(mJson))
          else
            None
        }
      )
    }
    val queryArgs = (p: PreparedStatement) => {
      p.setInt(1, limit)
      p.setInt(2, offset)
    }

    db.getRows[SubscriberRow](q, queryArgs, parser = parser)
  }

  def getSubscriberCount(): Try[Int] = {
    val q = s"""
        SELECT COUNT(email) as count FROM ${subscriberTableName}
    """
    db.getCount(q)
  }

  def updateSubscribersLastDigestTime(subscriberEmails: Array[String]) = Try {
    // TODO use temp table
    // https://javabydeveloper.com/spring-jdbctemplate-in-clause-with-list-of-values/
    val q = s"""
      UPDATE ${subscriberTableName}
      SET last_digest_at = timezone('utc'::text, now())
      WHERE email IN (${subscriberEmails.map(_ => "?").mkString(", ")})
      """
    val queryArgs = (p: PreparedStatement) => {
      for ((email, i) <- subscriberEmails.zipWithIndex) {
        p.setString(i + 1, email)
      }
    }

    log.info(s"Updating subscriber digest times.")
    db.applyUpdate(q, queryArgs)
  }

  def subscriberExists(email: String): Try[Boolean] = {
    val q = s"""
        SELECT COUNT(email) 
        FROM ${subscriberTableName}
        WHERE email = ?
    """
    val qa = (p: PreparedStatement) => {
      p.setString(1, email)
    }
    db.getCount(q, qa) match {
      case Success(c) => Success(c > 0)
      case _          => throw new RuntimeException("unable to get count")
    }
  }

  def upsertSubscriber(sd: SignupData): Try[Int] = {
    val q = s"""
        INSERT INTO ${subscriberTableName}(email, frequency)
        VALUES (?, ?)
        ON CONFLICT (email) DO 
            UPDATE SET frequency = EXCLUDED.frequency 
    """
    val queryArgs = (p: PreparedStatement) => {
      p.setString(1, sd.email)
      p.setString(2, sd.frequency.toString)
    }
    log.info(s"Upserting subscriber ${sd.toString} to persistant subscribers.")
    db.applyUpdate(q, queryArgs)
  }

  def removeSubscriber(sd: SignupData): Try[Int] = {
    val q = s"""
    DELETE FROM ${subscriberTableName}
    WHERE email = ?
    """
    val queryArgs = (p: PreparedStatement) => {
      p.setString(1, sd.email)
    }
    log.info(s"Upserting subscriber ${sd.toString} to persistant subscribers.")
    db.applyUpdate(q, queryArgs)
  }
}
