package fyi.newssnips.datacruncher.datastore

import com.typesafe.scalalogging.Logger

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._

import configuration.AppConfig
import java.sql.DriverManager
import java.util.Properties
import org.apache.spark.sql.{SaveMode, SparkSession}
import scala.util.Properties
import scala.util.Try
import fyi.newssnips.shared.DbConstants

class SparkPostgres(spark: SparkSession) {
  val log = Logger("app." + this.getClass().toString())

  private val keySpace    = DbConstants.keySpace
  private val KVTableName = DbConstants.KVTableName
  classOf[org.postgresql.Driver]
  val connectionProps = new Properties()
  connectionProps.setProperty("driver", "org.postgresql.Driver")

  private def init() = {
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      val statement = conn.createStatement()
      statement.executeUpdate(s"""
      CREATE SCHEMA IF NOT EXISTS ${keySpace};
      """)
      statement.executeUpdate(s"""
    CREATE TABLE IF NOT EXISTS ${KVTableName} (
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
      df.repartition(2)
        .write
        .mode(SaveMode.Overwrite)
        .option("numPartitions", 2)
        .jdbc(
          AppConfig.settings.pg.connStr,
          s"${keySpace}.${tableName}",
          connectionProps
        )
    }

  def getDataframe(tableName: String): Try[DataFrame] = Try {
    log.info(s"Fetching dataframe in table ${keySpace}.${tableName}.")
    val df = spark.read
      .option("numPartitions", 2)
      .jdbc(
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
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      val statement = conn
        .prepareStatement(
          s"""
          INSERT INTO $KVTableName (key, value)
          VALUES(?, ?)
          ON CONFLICT (key) DO 
            UPDATE SET value = ?;
         """
        )
      statement.setString(1, key)
      statement.setString(2, value)
      statement.setString(3, value)
      statement.executeUpdate()
    } finally {
      conn.close()
    }
  }

  def getKV(key: String): Try[String] = Try {
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      val statement = conn
        .prepareStatement(
          s"""
          SELECT value FROM $KVTableName WHERE key = ?;
         """
        )
      statement.setString(1, key)
      val rs = statement.executeQuery()
      rs.getString("value")
    } finally {
      conn.close()
    }
  }

  def deleteKV(key: String): Try[Unit] = Try {
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      val statement = conn
        .prepareStatement(
          s"DELETE FROM $KVTableName WHERE key = ?;"
        )
      statement.setString(1, key)
      statement.executeUpdate()
    } finally {
      conn.close()
    }
  }
}
