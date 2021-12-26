package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import javax.inject.Inject
import javax.inject.Named
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import play.api.inject.SimpleModule
import play.api.inject._
import javax.inject.Inject
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import java.io._
import java.lang.Runnable
import play.libs.Akka
import play.api._
import play.api.mvc._
import play.libs.Akka
import akka.actor._
import scala.concurrent.duration._
import play.api.libs.json.JsValue
import play.api.libs.json.JsPath

@Singleton
class RSSController @Inject() (
    val controllerComponents: ControllerComponents
) extends BaseController {

  def triggerReport(lookback: String) = Action {
    implicit request: Request[AnyContent] =>
      /*
      - get all URLs from DB.
      - fetch  XML content.
      - filter by content lookback date.
      - extract main entities from headings.
      - extract main words from content.
      - find ratio of negative to positive headings.
      - find ratio of negative to positive content.
      - send report to email and return OK
       */
      Ok(s"lookback: $lookback trigger not yet implemented")
  }

  def listURLs() = Action { implicit request: Request[AnyContent] =>
    Ok("list of urls")
  }

  def addURL() = Action(parse.json) { request =>
    val reqData: JsValue = request.body
    val url = (reqData \ "url").as[String]
    Ok(
      s"added $url"
    )
  }

  def removeURL() = Action(parse.json) { request =>
    val reqData: JsValue = request.body
    val url = (reqData \ "url").as[String]
    Ok(
      s"removed $url"
    )
  }
}
