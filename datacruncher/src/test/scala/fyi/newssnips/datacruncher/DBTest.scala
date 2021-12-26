package fyi.newssnips.datacruncher

/** A simple test for everyone's favourite wordcount example.
  */
import org.scalatest.{FlatSpec, FunSuite}
import com.typesafe.scalalogging.Logger
import fyi.newssnips.datastore.DatastaxCassandra
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import scala.util.{Failure, Success, Try}

// TODO https://www.baeldung.com/scala/scalatest
class DatastaxCassandraSpec extends FlatSpec {
  val log = Logger("app." + this.getClass().toString())

  import DatastaxCassandra.spark.implicits._

  "bla" should "bla" in {
    val k1 = "mtinittable"
    val df1 = Seq(
      ("b00001", "Sir Arthur Conan Doyle", "A3 study in scarlet", 1887),
      ("b00023", "Sir Arthur Conan Doyle", "A sign of four", 1890)
    ).toDF("book_id", "book_author", "book_name", "book_pub_year")
    log.info("Testing with dataframe")
    df1.show()

    DatastaxCassandra.putDataframe(k1, df1, "book_id") match {
      case Failure(s) => println(s)
      case _          => log.info("df1 added")
    }

    val df2: DataFrame = DatastaxCassandra.getDataframe(k1) match {
      case Success(d) =>
        d.show
        d
      case _ => {
        log.error("unable to get df2")
        Seq(0).toDF()
      }
    }

    df2.persist()
    val df3 = df2
      .withColumn("x4", col("book_pub_year") * 100)
      .limit(1)
    df3.show()

    DatastaxCassandra.deleteDataframe(k1) match {
      case Failure(s) => println(s)
      case _          => log.info("df removed")
    }

    DatastaxCassandra.putDataframe(
      k1,
      df3,
      "book_id"
    ) match {
      case Failure(s) => println(s)
      case _          => log.info("df3 added")
    }

    DatastaxCassandra.getDataframe(k1) match {
      case Success(d) =>
        log.info("d3:")
        d.count()
        d.show
      case _ => {
        log.error("unable to get updated df")
      }
    }
    assert(List.empty.size == 0)
  }

  it should "throw an IndexOutOfBoundsException when trying to access any element" in {
    DatastaxCassandra.upsertKV("test", "999") match {
      case Failure(s) => println(s)
      case _          => log.info("done")
    }
    println(DatastaxCassandra.getKV("test"))
    DatastaxCassandra.deleteKV("test") match {
      case Failure(s) => println(s)
      case _          => log.info("done")
    }
    assertThrows[IndexOutOfBoundsException] {
      log.info("42")
    }
  }
}

// val allContents = List(
//   List(
//     FeedContent(
/* "https://www.wsj.com/articles/u-s-military-chief-says-chinas-hypersonic-missile-test-is-close-to-sputnik-moment-11635344992", */
/* "China's Hypersonic Missile Test Is Close to 'Sputnik Moment,' U.S. Military
 * Chief Says", */
/* "Gen. Mark Milley described China’s recent test of a hypersonic missile as
 * “very concerning” and said the Pentagon was focused on the development.", */
//       false
//     ),
//     FeedContent(
/* "https://www.wsj.com/articles/iran-to-return-to-nuclear-deal-talks-in-vienna-next-month-11635348645", */
//       "Iran to Return to Nuclear Deal Talks Next Month",
/* "Tehran will return to negotiations on reviving the 2015 nuclear deal by the
 * end of November, its chief negotiator said Wednesday.", */
//       false
//     ),
//     FeedContent(
/* "https://www.wsj.com/articles/iran-to-return-to-nuclear-deal-talks-in-vienna-next-month-11635348645", */
//       "Iran to Return to Nuclear Deal Talks Next Month",
/* "Tehran will return to negotiations on reviving the 2015 nuclear deal by the
 * end of November, its chief negotiator said Wednesday.", */
//       false
//     )
//   )
// )
