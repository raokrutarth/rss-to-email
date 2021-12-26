package fyi.newssnips.webapp.datastore

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
import org.apache.spark.SparkContext
import com.redislabs.provider.redis._

@Singleton
class Cache(spark: SparkSession) {
  // FIXME: not working with exception:
  // redis.clients.jedis.exceptions.JedisConnectionException: Could not get a resource from the pool
  // https://github.com/RedisLabs/spark-redis/issues/325
  val log = Logger(this.getClass())

  // https://github.com/RedisLabs/spark-redis/blob/master/doc/dataframe.md
  spark.conf.set("spark.redis.host", AppConfig.settings.redis.host)
  spark.conf.set("spark.redis.port", AppConfig.settings.redis.port)
  spark.conf.set("spark.redis.auth", AppConfig.settings.redis.password)
  spark.conf.set("spark.redis.ssl", AppConfig.settings.redis.useTls)
  spark.conf.set("spark.redis.timeout", 5000)

  def putDf(key: String, df: DataFrame, ttl: Int = 30) = Try {
    df.write
      .format("org.apache.spark.sql.redis")
      .option("table", key)
      .option("ttl", 30)
      .option("model", "binary")
      .mode(SaveMode.Overwrite)
      .save()
  }

  def getDf(key: String): Try[DataFrame] = Try {
    spark.read
      .format("org.apache.spark.sql.redis")
      .option("table", key)
      .option("infer.schema", true)
      .load()
  }
}
