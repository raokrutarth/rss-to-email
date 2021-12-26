package fyi.newssnips.webapp.core.db

import com.typesafe.scalalogging.Logger
import configuration.AppConfig
import java.sql.ResultSet
import fyi.newssnips.shared.DbConstants
import scala.util._
import fyi.newssnips.shared._
import com.zaxxer.hikari._
import java.sql.PreparedStatement
import scala.reflect.ClassTag

// http://edulify.github.io/play-hikaricp.edulify.com/
object Postgres {
  val log = Logger("app." + this.getClass().toString())

  private val keySpace    = DbConstants.keySpace
  private val KVTableName = DbConstants.KVTableName
  private lazy val connectionPool = {
    log.info("Initiating PG connection pool.")

    val c = new HikariConfig()
    c.setJdbcUrl(AppConfig.settings.pg.connStr)
    c.setMaximumPoolSize(3)
    c.setAutoCommit(true)
    c.setDriverClassName("org.postgresql.Driver")
    c.setSchema(keySpace)
    new HikariDataSource(c)
  }

  classOf[org.postgresql.Driver]

  /** Get value of the given key from the KV table.
    */
  def getKv(key: String): Try[Option[String]] = Try {
    val conn = connectionPool.getConnection
    log.info(s"Fetching value for key ${key}")
    try {
      val statement = conn
        .prepareStatement(
          s"SELECT value FROM ${KVTableName} WHERE key = ?;"
        )
      statement.setString(1, key)
      val res = statement.executeQuery()
      if (res.next == false) {
        None
      } else {
        Some(res.getString("value"))
      }
    } catch {
      case e: Exception =>
        log.error(s"Failed to get key-val for key $key with exception $e")
        throw e
    } finally {
      conn.close()
    }
  }

  // returns the number of rows modified
  def applyUpdate(
      query: String,
      queryArgs: (PreparedStatement) => Unit = _ => ()
  ): Try[Int] = Try {
    val conn = connectionPool.getConnection
    try {
      val statement = conn
        .prepareStatement(query)
      queryArgs(statement)
      val n = statement.executeUpdate()
      log.info(s"Updated ${n} row(s) with query ${query}")
      n
    } catch {
      case e: Exception =>
        log.error(s"Failed to run update query ${query} with exception ${e}")
        throw e
    } finally {
      conn.close()
    }
  }

  /** Loads rows (if any) for the given read query into an array. Careful of memory overflow.
    *
    * @param query
    *   The string query
    * @param queryArgs
    *   Function that takes in a prepared statement and sets the variables in the query using the
    *   .set methods. Default is a NO-OP.
    * @param parser
    *   converts a row to the needed case class object.
    * @return
    *   array of rows
    */
  def getRows[A: ClassTag](
      query: String,
      queryArgs: PreparedStatement => Unit = _ => (),
      parser: ResultSet => A
  ): Try[Array[A]] = Try {
    val conn = connectionPool.getConnection
    try {
      val statement = conn
        .prepareStatement(query)
      queryArgs(statement)
      val res = statement.executeQuery()

      var rows = Array[A]()
      while (res.next) {
        rows = rows :+ parser(res)
      }
      log.info(s"Fetched ${rows.size} row(s) for query ${query}")
      rows
    } catch {
      case e: Exception =>
        log.error(s"Failed to get results for query ${query} with exception ${e}")
        throw e
    } finally {
      conn.close()
    }
  }

  def getCount(
      query: String,
      queryArgs: PreparedStatement => Unit = _ => ()
  ): Try[Int] = Try {
    val conn = connectionPool.getConnection
    try {
      val statement = conn
        .prepareStatement(query)
      queryArgs(statement)
      val res = statement.executeQuery()
      if (res.next) {
        res.getInt(1)
      } else {
        throw new RuntimeException("Query returned no count")
      }
    } catch {
      case e: Exception =>
        log.error(s"Failed to get results for query ${query} with exception ${e}")
        throw e
    } finally {
      conn.close()
    }
  }

  def cleanup() = {
    log.warn("Shutting down pg connection pool.")
    connectionPool.close()
  }
}
