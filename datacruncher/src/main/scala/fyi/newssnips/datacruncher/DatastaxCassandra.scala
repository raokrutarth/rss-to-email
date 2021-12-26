package fyi.newssnips.datastore

import com.typesafe.scalalogging.Logger
import com.datastax.spark.connector._

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import scala.util.{Failure, Success, Try}

import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.writer._

import javax.inject._
import configuration.AppConfig

@Singleton
object DatastaxCassandra {
  // https://github.com/datastax/spark-cassandra-connector/blob/master/doc/0_quick_start.md
  /* https://github.com/datastax/spark-cassandra-connector/blob/master/doc/5_saving.md#saving-rdds-as-new-tables */
  /* https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md#cassandra-connection-parameters */
  // https://github.com/datastax/spark-cassandra-connector/blob/master/doc/data_source_v1.md
  // https://github.com/datastax/spark-cassandra-connector/blob/master/doc/14_data_frames.md
  // https://spark.apache.org/docs/latest/sql-data-sources-load-save-functions.html
  // https://www.programcreek.com/scala/?api=com.datastax.spark.connector.cql.CassandraConnector
  // https://www.programcreek.com/scala/com.datastax.driver.core.Row
  /* https://datastax.github.io/spark-cassandra-connector/ApiDocs/3.1.0/connector/com/datastax/spark/connector/index.html */
  // https://spark.apache.org/docs/latest/api/scala/org/apache/spark/sql/DataFrameWriterV2.html
  // https://docs.microsoft.com/en-us/azure/cosmos-db/cassandra/spark-create-operations
  // https://docs.datastax.com/en/dse/6.8/dse-dev/datastax_enterprise/spark/sparkSqlJava.html
  private val keySpace             = if (AppConfig.settings.inProd) "prod" else "dev"
  private val cassandraCatalogName = "datastaxCassandra"
  private val KVTableName          = "key_value_udepqrn4g8s"
  val log                          = Logger(this.getClass())

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

  val cdbConnector = CassandraConnector(spark.sparkContext.getConf)

  cdbConnector.withSessionDo(session =>
    session.execute(
      s"""
        CREATE TABLE IF NOT EXISTS $keySpace.$KVTableName (
          key text PRIMARY KEY, 
          value text, 
        );
      """
    )
  )

  /* replaces the table in the DB with the one provided. idCol has to have unique values. */
  def putDataframe(tableName: String, df: DataFrame, idCol: String): Try[Boolean] = Try {
    log.info(
      s"Saving dataframe as table $tableName and ID column $idCol containing ${df.count()} rows."
    )
    df
      .writeTo(s"$cassandraCatalogName.$keySpace.$tableName")
      .partitionedBy(col(idCol))
      .createOrReplace()

    // has the overwrite vs. append flexibility but leads to stale schemas
    // df.write
    //   .mode("overwrite")
    //   .format("org.apache.spark.sql.cassandra")
    //   .options(Map("table" -> tableName, "keyspace" -> keySpace, "confirm.truncate" -> "true"))
    //   .save()
    true
  }

  def getDataframe(tableName: String): Try[DataFrame] = Try {
    log.info(s"Fetching dataframe in table $tableName.")
    spark.read
      .format("org.apache.spark.sql.cassandra")
      .options(Map("table" -> tableName, "keyspace" -> keySpace))
      .load()
    // spark.read.table(s"$cassandraCatalogName.$keySpace.$tableName")
  }

  /* CAUTION: can delete tables that are not dataframes. */
  def deleteDataframe(tableName: String): Try[Boolean] = Try {
    log.info(s"Deleting dataframe with table name $tableName")
    cdbConnector.withSessionDo(session =>
      session.execute(s"DROP TABLE IF EXISTS $keySpace.$tableName;")
    )
    true
  }

  def upsertKV(key: String, value: String): Try[Boolean] = Try {

    Seq((key -> value))
      .toDF("key", "value")
      .writeTo(s"$cassandraCatalogName.$keySpace.$KVTableName")
      .append()
    true
  }

  def getKV(key: String): Try[String] = Try {
    spark.read
      .format("org.apache.spark.sql.cassandra")
      .options(Map("table" -> KVTableName, "keyspace" -> keySpace))
      .load
      .filter(s"key = '$key'")
      .collect()
      .map(r => r(1))
      .head
      .asInstanceOf[String]
  }

  def deleteKV(key: String): Try[Boolean] = Try {
    cdbConnector.withSessionDo(session =>
      session.execute(
        s"""
      DELETE FROM 
        $keySpace.$KVTableName
      WHERE key = '$key';
    """
      )
    )
    true
  }

  // booksUpsertDF.createCassandraTable(
  //   keySpace,
  //   "books",
  //   partitionKeyColumns = Some(Seq("book_id")),
  //   clusteringKeyColumns = Some(Seq("book_author"))
  // )

// Upsert is no different from create

  // val x = "4-4-9"
  // spark.sql(
  //   s"""
  //     DELETE FROM $cassandraCatalogName.$keySpace.books
  //     WHERE book_id = 'b00501';
  //   """
  // )

  // .write
  //   .format("org.apache.spark.sql.cassandra")
  //   .options(Map("table" -> "books", "keyspace" -> keySpace))
  //   .mode("append")
  //   .save()

  def cleanup() = spark.stop()
}
