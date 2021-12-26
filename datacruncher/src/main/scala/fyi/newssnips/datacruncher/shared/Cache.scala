package fyi.newssnips.datastore

import com.typesafe.scalalogging.Logger
import scala.util.Try

import javax.inject._
import configuration.AppConfig
import fyi.newssnips.core.CategoryAnalysisPageData
import fyi.newssnips.models._
import _root_.redis.clients.jedis.JedisPool
import play.api.libs.json._

// TODO: move to play redis cache for simpler APIs
// https://github.com/KarelCemus/play-redis/issues/251
// https://github.com/KarelCemus/play-redis/issues/148#issuecomment-362821548
// https://github.com/KarelCemus/play-redis/issues/148#issuecomment-397617906

@Singleton
class Cache() {
  private val log = Logger("app." + this.getClass().toString())

  // add play redis cache https://www.baeldung.com/scala/play-caching and store arrays.

  private val keyspace: String = if (AppConfig.settings.inProd) "prod." else "dev."

  val redisPool = new JedisPool(AppConfig.settings.redis.url)

  def set(key: String, value: String) = Try {
    redisPool
      .getResource()
      .set(
        keyspace + key,
        value
      )
  }

  def get(key: String) = Try {
    val v = redisPool.getResource().get(keyspace + key)
    if (v.isEmpty) throw new RuntimeException(s"$key in cache empty")
    v
  }

  def flushCache() = Try {
    redisPool.getResource().flushAll()
  }

  def cleanup() = {
    log.warn("Shutting connections to cache connection pool.")
    redisPool.close()
  }
}
