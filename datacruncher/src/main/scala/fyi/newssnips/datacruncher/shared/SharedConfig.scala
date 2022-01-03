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
    pg: PostgresConfig,
    adminHttpAuth: AdminAuth
)
case class PostgresConfig(
    jdbcUrl: String
)

case class AdminAuth(
    username: String,
    password: String
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
  private val secrets: String =
    Properties.envOrNone("SHARED_SECRETS_B64") match {
      case Some(b64Contents) =>
        // Running in heroku. read b64 encoded secrets file.
        log.info("Using secrets provided from b64 env vars.")
        new String(Base64.getDecoder.decode(b64Contents))
      case _ =>
        // In dev/test mode. Use local/provided file.
        // can specify the config file path via env var or the
        // secrets.conf file in the current directory is used.
        val reader = Source.fromFile(
          Properties.envOrElse(
            "SHARED_SECRETS_FILE_PATH",
            "/home/dev/work/shared.secrets.conf"
          )
        )
        val contents: String = reader.getLines.mkString("\n")
        reader.close()
        contents
    }
  log.info("Initalizing shared config.")
  val config: SharedConfig = load(
    ConfigFactory.parseString(secrets)
  )

  private def getPostgresConfig(
      config: Config,
      inProd: Boolean
  ): PostgresConfig = {
    val dbProvider = if (inProd) "cockroachdb" else "yugabyte"

    log.info(s"Using postgres provider : ${dbProvider}")

    val user     = config.getString(s"database.$dbProvider.user")
    val password = config.getString(s"database.$dbProvider.password")
    val host     = config.getString(s"database.$dbProvider.host")
    val port     = config.getString(s"database.$dbProvider.port")
    val certPath = Properties.envOrNone("PG_CERT_B64") match {
      case Some(b64) =>
        val certPath = s"/tmp/${dbProvider}.crt"
        val os       = new FileOutputStream(certPath)
        os.write(Base64.getDecoder.decode(b64).toArray)
        os.close()
        log.info(
          s"Created db cert filefrom B64 at $certPath for provider ${dbProvider}"
        )
        certPath
      case _ =>
        Properties.envOrElse(
          "PG_CERT_PATH",
          s"/home/dev/work/${dbProvider}_db.crt"
        )
    }
    val db = config.getString(s"database.$dbProvider.database")

    PostgresConfig(
      s"jdbc:postgresql://${host}:${port}" +
        s"/${db}?user=${user}&password=${password}&ssl=true" +
        s"&sslmode=verify-full&sslrootcert=${certPath}"
    )
  }

  private def extractRedisConfig(
      config: Config,
      inProd: Boolean
  ): RedisConfig = {
    // TODO move to redis case class
    // convert the redis connection URL provided by heroku to
    // it's individual parts to feed into spark config.
    // e.g. rediss://:ioiuiy67t@ecty6.compute-1.amazonaws.com:28798

    val provider = if (inProd) "upstash" else "heroku"
    val useSsl   = if (inProd) true else false

    log.info(s"Using cache provider $provider with SSL $useSsl.")
    val connUrl: String = Properties.envOrNone("REDIS_URL") match {
      case Some(url) =>
        // give priority to redis provided by env var
        log.warn(s"Overriding cache provider with env var redis URI.")
        url
      case _ => config.getString(s"cache.redis.${provider}.uri")
    }

    val halfs    = connUrl.split("@")
    val password = halfs(0).split(":").last.strip()
    val host     = halfs(1).split(":").head.strip()
    val port     = halfs(1).split(":").last.strip().toInt

    RedisConfig(
      url = connUrl,
      host = host,
      port = port.toInt,
      password = password,
      useTls = useSsl
    )
  }

  def load(config: Config): SharedConfig = {
    val runtime: String = Properties.envOrElse("RUNTIME_ENV", "development")
    val inProd          = if (runtime.equals("docker")) true else false

    SharedConfig(
      runtimeEnv = runtime,
      inProd = inProd,
      redis = extractRedisConfig(config, inProd),
      timezone = Properties.envOrElse("TZ", "America/Los_Angeles"),
      pg = getPostgresConfig(config, inProd),
      adminHttpAuth = AdminAuth(
        username = config.getString("http_auth.admin.user"),
        password = config.getString("http_auth.admin.password")
      )
    )
  }
}
