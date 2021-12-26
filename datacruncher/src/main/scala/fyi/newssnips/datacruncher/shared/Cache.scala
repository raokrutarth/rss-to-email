package fyi.newssnips.datastore

import com.typesafe.scalalogging.Logger
import scala.util.Try

import javax.inject._
import configuration.AppConfig
import _root_.redis.clients.jedis.JedisPool
import _root_.redis.clients.jedis.Jedis
import scala.collection.JavaConverters._

// TODO: move to play redis cache for simpler APIs
// https://github.com/KarelCemus/play-redis/issues/251
// https://github.com/KarelCemus/play-redis/issues/148#issuecomment-362821548
// https://github.com/KarelCemus/play-redis/issues/148#issuecomment-397617906

@Singleton
class Cache() {
  private val log = Logger("app." + this.getClass().toString())

  // add play redis cache https://www.baeldung.com/scala/play-caching and store arrays.

  private val keyspace: String =
    if (AppConfig.settings.inProd) "prod." else "dev."

  // https://www.javadoc.io/doc/redis.clients/jedis/3.7.0/redis/clients/jedis/Jedis.html
  val redisPool = {
    log.info("Initiating cache connection pool.")
    new JedisPool(AppConfig.settings.redis.url)
  }

  // https://github.com/redis/jedis/issues/2708
  def set(key: String, value: String, exSec: java.lang.Integer = 0) = Try {
    val r: Jedis = redisPool.getResource()
    if (exSec > 0) {
      r.setex(keyspace + key, exSec, value)
    } else {
      r.set(keyspace + key, value)
    }
  }

  def delete(key: String) = Try {
    redisPool.getResource().del(keyspace + key)
  }

  def get(key: String) = Try {
    val v = redisPool.getResource().get(keyspace + key)
    if (v.isEmpty) throw new RuntimeException(s"No value for $key in cache.")
    v
  }

  def flushCache() = Try {
    val r = redisPool.getResource()
    log.info(s"Removeing all keys in cache for keyspace ${keyspace}")
    r.keys(keyspace + "*").asScala.map(k => r.del(k))
  }

  def cleanup() = {
    log.warn("Shutting connections to cache connection pool.")
    redisPool.close()
  }
}
