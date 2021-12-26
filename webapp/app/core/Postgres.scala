package fyi.newssnips.webapp.core

import com.typesafe.scalalogging.Logger
import configuration.AppConfig
import java.sql.{DriverManager, ResultSet}
import fyi.newssnips.shared.DbConstants
import scala.util._
import fyi.newssnips.shared._
import java.sql.{Connection, Statement}
import com.zaxxer.hikari._
import fyi.newssnips.models._
import java.sql.PreparedStatement

// http://edulify.github.io/play-hikaricp.edulify.com/
object Postgres {
  val log = Logger("app." + this.getClass().toString())

  private val keySpace    = DbConstants.keySpace
  private val KVTableName = DbConstants.KVTableName
  private lazy val connectionPool = {
    val c = new HikariConfig()
    c.setJdbcUrl(AppConfig.settings.pg.connStr)
    c.setMaximumPoolSize(3)
    c.setAutoCommit(true)
    c.setDriverClassName("org.postgresql.Driver")
    c.setSchema(keySpace)
    new HikariDataSource(c)
  }

  // scalafix:ok
  classOf[org.postgresql.Driver]

  /** Get value of the given key from the KV table.
    */
  def getKv(key: String): Try[Option[String]] = Try {
    val conn = connectionPool.getConnection
    log.info(s"Fetching value for key ${key}")
    try {
      val statement = conn
        .prepareStatement(
          s"SELECT * FROM ${KVTableName} WHERE key = ?;"
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
  def getRows[A: ClassManifest](
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
        throw e
    } finally {
      conn.close()
    }
  }

  def getAnalysisRows(
      categoryMetadata: CategoryDbMetadata,
      limit: Int = 100,
      offset: Int = 0
  ): Try[Array[AnalysisRow]] = {
    val q = s"""
      SELECT * FROM ${categoryMetadata.analysisTableName}
      ORDER BY "totalNumTexts" DESC 
      LIMIT ? OFFSET ?;
    """
    val parser = (r: ResultSet) => {
      AnalysisRow(
        entityName = r.getString("entityName"),
        entityType = r.getString("entityType"),
        negativeMentions = r.getLong("negativeMentions"),
        positiveMentions = r.getLong("positiveMentions"),
        totalNumTexts = r.getLong("totalNumTexts"),
        positiveTextIds = None,
        negativeTextIds = None
      )
    }
    val queryArgs = (p: PreparedStatement) => {
      p.setInt(1, limit)
      p.setInt(2, offset)
    }
    getRows[AnalysisRow](q, queryArgs, parser)
  }

  def getFeedsRows(categoryMetadata: CategoryDbMetadata): Try[Array[FeedRow]] = Try {
    val conn = connectionPool.getConnection
    try {
      val rs = conn
        .createStatement(
          ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY
        )
        .executeQuery(
          s"SELECT * FROM ${categoryMetadata.sourceFeedsTableName};"
        )

      var rows = Array[FeedRow]()
      while (rs.next) {
        rows = rows :+ FeedRow(
          feed_id = rs.getLong("feed_id"),
          url = rs.getString("url"),
          title = rs.getString("title"),
          last_scraped = rs.getString("last_scraped")
        )
      }
      log.info(s"Fetch feeds table with ${rows.size} rows.")
      rows
    } catch {
      case e: Exception =>
        throw e
    } finally {
      conn.close()
    }
  }

  def getTexts(
      categoryMetadata: CategoryDbMetadata,
      entityName: String,
      entityType: String,
      sentiment: String
  ): Try[Array[TextsPageRow]] = Try {
    /* // 3 table join https://kb.objectrocket.com/postgresql/join-three-tables-in-postgresql-539
     * aray contains
     * https://stackoverflow.com/questions/39643454/postgres-check-if-array-field-contains-value */

    val logIdentifier =
      Seq(entityType, entityName, sentiment).mkString(
        " - "
      ) + categoryMetadata.toString()
    log.info(s"Fetching texts for ${logIdentifier}")

    val conn = connectionPool.getConnection
    val analysisIdsCol = sentiment.trim.toLowerCase match {
      case "negative" | "neg" => "negativeTextIds"
      case "positive" | "pos" => "positiveTextIds"
      case _                  => "UNKNOWN"
    }
    try {

      // Join the URL and text tables for the given entity and sentiment
      // to pick the right texts, explode/unnest the column
      // in the analysis table to get the text IDs
      val q = s""" 
      SELECT
          urls.url AS url,
          texts.text AS text
      FROM
          ${categoryMetadata.articleUrlsTableName} urls
          INNER JOIN ${categoryMetadata.textsTableName} texts 
            ON texts.link_id = urls.link_id
            WHERE texts.text_id IN (
              SELECT unnest("${analysisIdsCol}")
              FROM
                ${categoryMetadata.analysisTableName} analysis
              WHERE
                analysis."entityName" = ? 
                AND analysis."entityType" = ?
              )
      """

      log.info(s"Fetching texts with query $q")
      val statement = conn
        .prepareStatement(q)
      statement.setString(1, entityName)
      statement.setString(2, entityType)
      val res = statement.executeQuery()

      var rows = Array[TextsPageRow]()
      while (res.next) {
        rows = rows :+ TextsPageRow(
          text = res.getString("text"),
          url = res.getString("url")
        )
      }
      log.info(s"Fetched ${rows.size} text(s) and their source(s) for ${logIdentifier}")
      rows
    } catch {
      case e: Exception =>
        log.error(s"Failed to get texts with exception $e")
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
