package controllers

import java.io._
import java.lang.Runnable
import javax.inject.Inject
import javax.inject.Named
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.duration._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import play.api._
import play.api._
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.concurrent.CustomExecutionContext
import play.api.mvc._
import play.api.mvc._
import play.libs.Akka

import configuration.AppConfig
// redis: https://levelup.gitconnected.com/dockerizing-scala-redis-nginx-c97d067244d9
// spark docs: https://spark.apache.org/docs/latest/api/scala/org/apache/spark/index.html
// scala + play tutorials: https://www.youtube.com/watch?v=FqMDHsFNlxQ&list=PLLMXbkbDbVt8tBiGc1y69BZdG8at1D7ZF

/** This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def testSpark() = Action { implicit request: Request[AnyContent] =>
    val spark: SparkSession =
      SparkSession
        .builder()
        .appName("RSS to Email")
        .master("local")
        .getOrCreate()

    val sv = spark.version
    println(s"Conencted to spark with version $sv")

    val logData = spark.read.textFile("/opt/docker/conf/logback.xml").cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    spark.stop()

    Ok(s"Lines with a: $numAs, Lines with b: $numBs")
  }
}
