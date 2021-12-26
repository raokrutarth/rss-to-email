package fyi.newssnips.datacruncher.datastore

import com.typesafe.scalalogging.Logger

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._

import configuration.AppConfig
import java.sql.{DriverManager, ResultSet}
import java.util.Properties
import org.apache.spark.sql.{SaveMode, SparkSession}
import scala.util.Properties
import scala.util.Try
import fyi.newssnips.shared.DbConstants

class SparkPostgres(spark: SparkSession) {
  val log = Logger("app." + this.getClass().toString())

  private val keySpace    = DbConstants.keySpace
  private val KVTableName = DbConstants.KVTableName

  val connectionProps = new Properties()
  connectionProps.setProperty("driver", "org.postgresql.Driver")

  import spark.implicits._

  def queryCheck() = {
    println("Postgres connector")
    classOf[org.postgresql.Driver]
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )

      val rs =
        stm.executeQuery(
          "SELECT * from users JOIN user_other_details ON users.name = user_other_details.name;"
        )

      while (rs.next) {
        println(rs.getString("name") + "---" + rs.getString("address"))
      }
    } finally {
      conn.close()
      spark.close()
    }
  }

  private def init() = {
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      val statement = conn.createStatement()
      statement.executeUpdate(s"""
      CREATE SCHEMA IF NOT EXISTS ${keySpace};
      """)
      statement.executeUpdate(s"""
    CREATE TABLE IF NOT EXISTS ${keySpace}.key_value_udepqrn4g8s (
      key text PRIMARY KEY,
      value text 
    );
    """)
    } finally {
      conn.close()
    }
  }
  init()

  def putDataframe(
      tableName: String,
      df: DataFrame
  ): Try[Unit] =
    Try {
      log.info(
        s"Saving dataframe as table ${keySpace}.$tableName containing ${df.count()} row(s)."
      )
      df.write
        .mode(SaveMode.Overwrite)
        .jdbc(
          AppConfig.settings.pg.connStr,
          s"${keySpace}.${tableName}",
          connectionProps
        )
    }

  def getDataframe(tableName: String): Try[DataFrame] = Try {
    log.info(s"Fetching dataframe in table ${keySpace}.${tableName}.")
    val df = spark.read.jdbc(
      AppConfig.settings.pg.connStr,
      s"${keySpace}.${tableName}",
      connectionProps
    )
    df
  }

  def deleteDataframe(tableName: String): Try[Unit] = Try {
    log.info(s"Deleting dataframe with table name ${keySpace}.${tableName}")
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      conn
        .createStatement()
        .executeUpdate(
          s"DROP TABLE IF EXISTS ${keySpace}.${tableName};"
        )
    } finally {
      conn.close()
    }
  }

  def upsertKV(key: String, value: String): Try[Unit] = Try {
    Seq((key -> value))
      .toDF("key", "value")
      .write
      .mode(SaveMode.Append)
      .jdbc(AppConfig.settings.pg.connStr, KVTableName, connectionProps)
  }

  def getKV(key: String): Try[String] = Try {
    spark.read
      .jdbc(
        AppConfig.settings.pg.connStr,
        KVTableName,
        connectionProps
      )
      .filter(s"key = '$key'")
      .collect()
      .map(r => r(1))
      .head
      .asInstanceOf[String]
  }

  def deleteKV(key: String): Try[Unit] = Try {
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      conn
        .createStatement()
        .executeUpdate(
          s"""
      DELETE FROM $KVTableName
      WHERE key = '$key';
    """
        )
    } finally {
      conn.close()
    }
  }
}
