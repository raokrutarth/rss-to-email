package controllers

import javax.inject.Inject
import javax.inject._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._

import play.api.mvc._
import fyi.newssnips.datastore.ConnectDatabase

@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def index() = Action {
    Redirect("/v1/home")
  }

  def about() = Action {
    Ok(
      views.html.about()
    ).as("text/html")
  }

  def testSpark() = Action {
    val spark: SparkSession =
      SparkSession
        .builder()
        .appName("RSS to Email")
        .master("local")
        .getOrCreate()

    val sv = spark.version
    println(s"Conencted to spark with version $sv")

    val logData = spark.read.textFile("/opt/docker/conf/logback.xml").cache()
    val numAs   = logData.filter(line => line.contains("a")).count()
    val numBs   = logData.filter(line => line.contains("b")).count()
    spark.stop()

    Ok(s"Lines with a: $numAs, Lines with b: $numBs")
  }
}
