package fyi.newssnips.webapp.core

import com.typesafe.scalalogging.Logger

import configuration.AppConfig
import java.sql.{DriverManager, ResultSet}
import scala.util.Try
import fyi.newssnips.shared.DbConstants
import scala.util.Success
import scala.util.Failure
import fyi.newssnips.shared._

object Postgres {
  val log = Logger("app." + this.getClass().toString())

  private val keySpace    = DbConstants.keySpace
  private val KVTableName = DbConstants.KVTableName
  classOf[org.postgresql.Driver]

  private def runGetQuery(query: String): Try[ResultSet] = Try {
    val conn =
      DriverManager.getConnection(AppConfig.settings.pg.connStr)
    try {
      conn
        .createStatement(
          ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY
        )
        .executeQuery(query)
    } finally {
      conn.close()
    }
  }
  val categoryMetadata: CategoryDbMetadata = DbConstants.categoryToDbMetadata("home")

  val urlTable   = s"${keySpace}.${categoryMetadata.articleUrlsTableName}"
  val textsTable = s"${keySpace}.${categoryMetadata.textsTableName}"

  // order by
  val rs = runGetQuery(s"""
    SELECT * 
    FROM 
        ${urlTable}
    JOIN
        ${textsTable}
    ON ${urlTable}.link_id = ${textsTable}.link_id;
    """)
  rs match {
    case Success(rows) =>
      while (rows.next) {
        log.info(Seq(rows.getString("text"), rows.getString("url")).mkString(" "))
      }
    case Failure(exception) => log.error(s"get failed with error ${exception}")
  }

  def queryCheck() = {
    log.info(s"Performing ")
  }
}
