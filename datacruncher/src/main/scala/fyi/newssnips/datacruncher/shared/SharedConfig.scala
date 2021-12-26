package fyi.newssnips.shared.config

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.FileOutputStream
import java.util.Base64
import scala.io.Source
import com.typesafe.scalalogging.Logger

// https://github.com/alexandru/scala-best-practices/blob/master/sections/3-architecture.md

case class SharedConfig(
    redis: RedisConfig,
    runtimeEnv: String,
    inProd: Boolean,
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/ZoneId.html
    timezone: String,
    pg: PostgresConfig
)
case class PostgresConfig(
    jdbcUrl: String
)

case class RedisConfig(
    url: String,
    host: String,
    port: Int,
    password: String,
    useTls: Boolean
)

object SharedConfig {
  val log: Logger = Logger("app." + this.getClass().toString())

  // no way to mount files in heroku so convert the
  // secrets to base64 string and decode from env var.
  private val secrets: String = Properties.envOrNone("SECRETS_B64") match {
    case Some(b64Contents) =>
      // Running in heroku. read b64 encoded secrets file.
      log.info("Using secrets provided from b64 env vars.")
      new String(Base64.getDecoder.decode(b64Contents))
    case _ =>
      // In dev/test mode. Use local/provided file.
      // can specify the config file path via env var or the
      // secrets.conf file in the current directory is used.
      val reader = Source.fromFile(
        Properties.envOrElse("SECRETS_FILE_PATH", "secrets.conf")
      )
      val contents: String = reader.getLines.mkString("\n")
      reader.close()
      contents
  }

  val settings: SharedConfig = load(
    ConfigFactory.parseString(secrets)
  )

  private def getPostgresConfig(config: Config): PostgresConfig = {
    // TODO move to pg case class
    val user     = config.getString("secrets.database.yugabyte.user")
    val password = config.getString("secrets.database.yugabyte.password")
    val host     = config.getString("secrets.database.yugabyte.host")
    val port     = config.getString("secrets.database.yugabyte.port")
    val certPath = Properties.envOrNone("YB_CERT_B64") match {
      case Some(b64) =>
        val certPath = "/tmp/yb_pg_cert.crt"
        val os       = new FileOutputStream(certPath)
        os.write(Base64.getDecoder.decode(b64).toArray)
        os.close()
        log.info(s"Created db cert filefrom B64 at $certPath")
        certPath
      case _ => "/home/dev/work/yugabyte_db_cert.crt"
    }
    val db = "newssnips"

    PostgresConfig(
      s"jdbc:postgresql://${host}:${port}" +
        s"/${db}?user=${user}&password=${password}&ssl=true" +
        s"&sslmode=verify-full&sslrootcert=${certPath}"
    )
  }

  private def extractRedisConfig(config: Config): RedisConfig = {
    // TODO move to redis case class
    // convert the redis connection URL provided by heroku to
    // it's individual parts to feed into spark config.
    // e.g. rediss://:ioiuiy67t@ecty6.compute-1.amazonaws.com:28798
    val connUrl: String = Properties.envOrNone("REDIS_URL") match {
      case Some(url) =>
        url
      case _ => config.getString("secrets.cache.redis.http_url")
    }

    val halfs    = connUrl.split("@")
    val password = halfs(0).split(":").last.strip()
    val host     = halfs(1).split(":").head.strip()
    val port     = halfs(1).split(":").last.strip().toInt

    RedisConfig(url = connUrl, host = host, port = port, password = password, useTls = false)
  }

  def load(config: Config): SharedConfig = {
    val runtime: String = Properties.envOrElse("RUNTIME_ENV", "development")

    SharedConfig(
      runtimeEnv = runtime,
      inProd = if (runtime.equals("docker")) true else false,
      redis = extractRedisConfig(config),
      timezone = Properties.envOrElse("TZ", "America/Los_Angeles"),
      pg = getPostgresConfig(config)
    )
  }
}
