package fyi.newssnips.datacruncher

import org.apache.spark.sql.SparkSession
import com.typesafe.scalalogging.Logger
import com.datastax.spark.connector._
import configuration.AppConfig
import org.apache.spark.rdd.RDD
//Spark connector
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.writer._

object DatastaxCassandra {
  // https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md#cassandra-connection-parameters

  private val keySpace             = if (AppConfig.settings.inProd) "prod" else "dev"
  private val cassandraCatalogName = "datastaxCassandra"
  // https://www.programcreek.com/scala/?api=com.datastax.spark.connector.cql.CassandraConnector
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("RSS to Email")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .config("spark.files", "/home/dev/work/datacruncher/datastax-db-secrets.zip")
      .config("spark.cassandra.connection.config.cloud.path", "datastax-db-secrets.zip")
      .config("spark.cassandra.auth.username", AppConfig.settings.database.clientId)
      .config("spark.cassandra.auth.password", AppConfig.settings.database.clientSecret)
      //   .config("spark.cassandra.connection.host", "f2414505-ba4f-4186-a9d2-e4c997a22540-westus2.db.astra.datastax.com")
      //   .config("spark.cassandra.connection.port", "29080")
      .config("spark.sql.extensions", "com.datastax.spark.connector.CassandraSparkExtensions")
      .config("spark.dse.continuousPagingEnabled", "false")
      .config("spark.cassandra.connection.remoteConnectionsPerExecutor", "10") // Spark 3.x
      .config("spark.cassandra.output.concurrent.writes", "1000")
      .config("spark.cassandra.concurrent.reads", "512")
      .config("spark.cassandra.output.batch.grouping.buffer.size", "1000")
      .config("park.cassandra.connection.keepAliveMS", "600000000")
      .config(
        s"spark.sql.catalog.$cassandraCatalogName",
        "com.datastax.spark.connector.datasource.CassandraCatalog"
      )
      .master("local[*]")
      .getOrCreate()

  import spark.implicits._

  //Cassandra connector instance
  val cdbConnector = CassandraConnector(spark.sparkContext.getConf)

  // cdbConnector.withSessionDo(session => session.execute(s"DROP TABLE IF EXISTS $keySpace.books; "))

  // cdbConnector.withSessionDo(session =>
  //   session.execute(
  //     s"CREATE TABLE IF NOT EXISTS $keySpace.books(book_id TEXT,book_author TEXT, " +
  //       s"book_name TEXT,book_pub_year INT,book_price FLOAT, " +
  //       s"PRIMARY KEY(book_id,book_pub_year));"
  //   )
  // )

  // val booksUpsertDF = Seq(
  //   ("b00001", "Sir Arthur Conan Doyle", "A3 study in scarlet", 1887)
  //   // ("b00023", "Sir Arthur Conan Doyle", "A sign of four", 1890),
  //   // ("b01001", "Sir Arthur Conan Doyle", "The adventures of Sherlock Holmes", 1892),
  //   // ("b00501", "Sir Arthur Conan Doyle", "The memoirs of Sherlock Holmes", 1893),
  //   // ("b00300", "Sir Arthur Conan Doyle", "The hounds of Baskerville", 1901),
  //   // ("b09999", "Sir Arthur Conan Doyle", "The return of Sherlock Holmes", 1905)
  // ).toDF("book_id", "book_author", "book_name", "book_pub_year")
  // booksUpsertDF.show()

  // booksUpsertDF.createCassandraTable(
  //   keySpace,
  //   "books",
  //   partitionKeyColumns = Some(Seq("book_id")),
  //   clusteringKeyColumns = Some(Seq("book_author"))
  // )

// Upsert is no different from create
  // booksUpsertDF
  //   .writeTo(s"$cassandraCatalogName.$keySpace.books")
  //   .append()
  // .partitionedBy($"book_id")
  // .createOrReplace()
  // val x = "4-4-9"
  // spark.sql(
  //   s"""
  //     DELETE FROM $cassandraCatalogName.$keySpace.books
  //     WHERE book_id = 'b00501';
  //   """
  // )
  spark.sparkContext
    .cassandraTable(keySpace, "books")
    .where("book_id='b00501'")
    .deleteFromCassandra(keySpace, "books")

  // .write
  //   .format("org.apache.spark.sql.cassandra")
  //   .options(Map("table" -> "books", "keyspace" -> keySpace))
  //   .mode("append")
  //   .save()

  spark.stop()

}
