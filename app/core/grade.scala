package core

import org.apache.spark.sql._
import org.apache.spark.sql.types._

case class TimeUsageRow(
    working: String,
    sex: String,
    age: String,
    primaryNeeds: Double,
    work: Double,
    other: Double
)

trait TimeUsageInterface {
  val spark: SparkSession
  def classifiedColumns(
      columnNames: List[String]
  ): (List[Column], List[Column], List[Column])
  def row(line: List[String]): Row
  def timeUsageGrouped(summed: DataFrame): DataFrame
  def timeUsageGroupedSql(summed: DataFrame): DataFrame
  def timeUsageGroupedSqlQuery(viewName: String): String
  def timeUsageGroupedTyped(
      summed: Dataset[TimeUsageRow]
  ): Dataset[TimeUsageRow]
  def timeUsageSummary(
      primaryNeedsColumns: List[Column],
      workColumns: List[Column],
      otherColumns: List[Column],
      df: DataFrame
  ): DataFrame
  def timeUsageSummaryTyped(
      timeUsageSummaryDf: DataFrame
  ): Dataset[TimeUsageRow]
}
