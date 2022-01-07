package fyi.newssnips.webapp.core.dal

import com.typesafe.scalalogging.Logger
import java.sql.ResultSet
import scala.util._
import fyi.newssnips.shared._
import fyi.newssnips.models._
import java.sql.PreparedStatement
import fyi.newssnips.webapp.core.db.Postgres
import javax.inject._

@Singleton
class PgAccess @Inject() (db: Postgres) {
  val log = Logger("app." + this.getClass().toString())

  def getAnalysisRows(
      categoryMetadata: CategoryDbMetadata,
      minPositivity: Int,
      limit: Int,
      offset: Int
  ): Try[Array[AnalysisRowUi]] = {
    val q = s"""
      SELECT 
        "entityName", "entityType", 
        "negativeMentions", "positiveMentions", 
        "totalNumTexts",
        ROUND(("positiveMentions"::float / "totalNumTexts"::float) * 100) AS positivity_score

      FROM ${categoryMetadata.analysisTableName}
      WHERE ROUND(("positiveMentions"::float / "totalNumTexts"::float) * 100) >= ?
      ORDER BY "totalNumTexts" DESC 
      LIMIT ? OFFSET ?;
    """
    val parser = (r: ResultSet) => {
      AnalysisRowUi(
        entityName = r.getString("entityName"),
        entityType = r.getString("entityType"),
        negativeMentions = r.getLong("negativeMentions"),
        positiveMentions = r.getLong("positiveMentions"),
        totalNumTexts = r.getLong("totalNumTexts"),
        positivityScore = r.getInt("positivity_score")
      )
    }
    val queryArgs = (p: PreparedStatement) => {
      p.setInt(1, minPositivity)
      p.setInt(2, limit)
      p.setInt(3, offset)
    }
    db.getRows[AnalysisRowUi](q, queryArgs, parser)
  }

  def getFeedsRows(categoryMetadata: CategoryDbMetadata): Try[Array[FeedRow]] = {
    // very small table. pagination not needed.
    val q = s"SELECT * FROM ${categoryMetadata.sourceFeedsTableName};"
    val parser = (r: ResultSet) => {
      FeedRow(
        feed_id = r.getLong("feed_id"),
        url = r.getString("url"),
        title = r.getString("title"),
        last_scraped = r.getString("last_scraped")
      )
    }
    db.getRows[FeedRow](query = q, parser = parser)
  }

  def getTexts(
      categoryMetadata: CategoryDbMetadata,
      entityName: String,
      entityType: String,
      sentiment: String,
      limit: Int = 100,
      offset: Int = 0
  ): Try[Array[TextsPageRow]] = {

    val logIdentifier =
      Seq(entityType, entityName, sentiment).mkString(
        " - "
      ) + categoryMetadata.toString()
    log.info(s"Fetching texts from db for ${logIdentifier}")

    val analysisIdsCol = sentiment.trim.toLowerCase match {
      case "negative" | "neg" => "negativeTextIds"
      case "positive" | "pos" => "positiveTextIds"
      case _ =>
        log.error(s"Unknown sentiment ${sentiment} during texts fetch for ${logIdentifier}")
        "UNKNOWN"
    }
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
      LIMIT ? OFFSET ?;
      """
    val queryArgs = (p: PreparedStatement) => {
      p.setString(1, entityName)
      p.setString(2, entityType)
      p.setInt(3, limit)
      p.setInt(4, offset)
    }
    val parser = (r: ResultSet) => {
      TextsPageRow(
        text = r.getString("text"),
        url = r.getString("url")
        // host = import java.net.URI | new URI(r.getString("url")).getHost()
      )
    }
    db.getRows[TextsPageRow](query = q, parser = parser, queryArgs = queryArgs)
  }
}
