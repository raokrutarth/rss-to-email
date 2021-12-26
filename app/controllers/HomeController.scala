package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

/** This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be
    * called when the application receives a `GET` request with a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    // <serviceName>.<namespaceName>.svc.cluster.local
    val spark: SparkSession =
      SparkSession
        .builder()
        .appName("RSS to Email")
        .master("local")
        // .master("spark://spark-master-svc.rte-ns.svc.cluster.local:7077")
        .getOrCreate()

    val sv = spark.version
    println(s"Conencted to spark with version $sv")

    val logData = spark.read.textFile("/opt/docker/conf/logback.xml").cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println(s"Lines with a: $numAs, Lines with b: $numBs")
    spark.stop()

    Ok(views.html.index())
  }
}
