package fyi.newssnips.webapp.datastore

import com.typesafe.scalalogging.Logger

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import scala.util.Try

import javax.inject._
import configuration.AppConfig
import org.apache.spark.storage.StorageLevel

@Singleton
class Cache(spark: SparkSession) {
  private val log = Logger("app." + this.getClass().toString())

  // https://github.com/RedisLabs/spark-redis/blob/master/doc/dataframe.md

  def putDf(key: String, df: DataFrame, ttl: Int = 300) = Try {
    log.info(s"Saving dataframe with ${df.count()} rows using key ${key} to cache.")

    df.persist(StorageLevel.DISK_ONLY) // FIXME saves empty df otherwise
    df.write
      .format("org.apache.spark.sql.redis")
      .option("table", key)
      .option("ttl", ttl)
      .option("model", "binary")
      .option("host", AppConfig.settings.redis.host)
      .option("port", AppConfig.settings.redis.port)
      .option("auth", AppConfig.settings.redis.password)
      .option("ssl", AppConfig.settings.redis.useTls)
      .option("timeout", 5000)
      .mode(SaveMode.Overwrite)
      .save()

    df.unpersist()
  }

  def removeDf(key: String) = Try {
    // TODO think of a better way to do this.
    log.info(s"Overwriting dataframe with key ${key} with empty data in cache.")

    spark.emptyDataFrame.write
      .format("org.apache.spark.sql.redis")
      .option("table", key)
      .option("model", "binary")
      .option("host", AppConfig.settings.redis.host)
      .option("port", AppConfig.settings.redis.port)
      .option("auth", AppConfig.settings.redis.password)
      .option("ssl", AppConfig.settings.redis.useTls)
      .option("timeout", 5000)
      .mode(SaveMode.Overwrite)
      .save()
  }

  def getDf(key: String): Try[DataFrame] = Try {
    val df = spark.read
      .format("org.apache.spark.sql.redis")
      .option("table", key)
      // .option("infer.schema", true)
      .option("host", AppConfig.settings.redis.host)
      .option("port", AppConfig.settings.redis.port)
      .option("auth", AppConfig.settings.redis.password)
      .option("ssl", AppConfig.settings.redis.useTls)
      .option("timeout", 5000)
      .option("model", "binary")
      .load()

    // df.persist(StorageLevel.DISK_ONLY) // FIXME loads empty df otherwise
    if (df.isEmpty) {
      log.warn(s"Cache miss on table $key.")
      throw new RuntimeException(s"Dataframe in cache for key $key is empty.")
    }

    log.info(s"Fetched dataframe with ${df.count()} rows using key ${key} from cache.")
    df
  }
}
