package fyi.newssnips.datastore

import com.typesafe.scalalogging.Logger
import scala.util.Try

import javax.inject._
import fyi.newssnips.shared.config.SharedConfig
import _root_.redis.clients.jedis._
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
    if (SharedConfig.config.inProd) "prod." else "dev."

  // https://www.javadoc.io/doc/redis.clients/jedis/3.7.0/redis/clients/jedis/Jedis.html
  val redisPool = {
    log.info("Initiating cache connection pool.")
    val c = new JedisPoolConfig()
    c.setMaxTotal(10)
    c.setMaxWaitMillis(2000)

    new JedisPool(
      c,
      SharedConfig.config.redis.host,
      SharedConfig.config.redis.port,
      2000, // timeout (sec)
      SharedConfig.config.redis.password,
      SharedConfig.config.redis.useTls
    )
  }

  // https://github.com/redis/jedis/issues/2708
  def set(key: String, value: String, exSec: java.lang.Integer = 0): Boolean = {
    try {
      val r: Jedis = redisPool.getResource()
      if (exSec > 0) {
        r.setex(keyspace + key, exSec, value)
        true
      } else {
        r.set(keyspace + key, value)
        true
      }
    } catch {
      case e: Exception =>
        log.error(s"Cache write failed with exception $e")
        false
    }
  }

  def delete(key: String) = Try {
    redisPool.getResource().del(keyspace + key)
  }

  def get(key: String): Option[String] = {

    try {
      val v = redisPool.getResource().get(keyspace + key)
      if (v == null || v.isEmpty) {
        log.info(s"No value for $key in cache.")
        None
      } else {
        Some(v)
      }
    } catch {
      case e: Exception =>
        log.error(s"Cache read failed with exception $e")
        None
    }
  }

  def flushCache() = Try {
    val r = redisPool.getResource()
    log.info(s"Removing all keys in cache for keyspace ${keyspace}")
    r.keys(keyspace + "*").asScala.map(k => r.del(k))
  }

  def cleanup() = {
    log.warn("Shutting connections to cache connection pool.")
    redisPool.close()
  }
}
