package configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.FileOutputStream
import java.util.Base64
import scala.io.Source
import com.typesafe.scalalogging.Logger

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
/* 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly */

case class AppConfig(
    database: DatastaxConfig,
    redis: RedisConfig,
    runtimeEnv: String,
    inProd: Boolean,
    sampleDfs: Boolean, // when enabled, prints the intermediate DFs (slow)

    // timezone of app from
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/ZoneId.html
    timezone: String,
    pg: PostgresConfig
)
case class PostgresConfig(
    connStr: String
)

case class DatastaxConfig(
    url: String,
    appToken: String,
    clientId: String,
    clientSecret: String,
    connectionPackagePath: String
)

case class RedisConfig(
    url: String,
    host: String,
    port: Int,
    password: String,
    useTls: Boolean
)

object AppConfig {
  // System.setProperty("logback.configurationFile", "/home/dev/work/webapp/conf/logback.xml")
  val log: Logger = Logger("app." + this.getClass().toString())

  // no way to mount files in heroku so convert the
  // secrets to base64 string and decode from env var.
  private val secrets: String = Properties.envOrNone("SECRETS_B64") match {
    case Some(b64Contents) =>
      // Running in heroku. read b64 encoded secrets file.
      log.info("Using secrets provided from heroku config vars.")
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

  val settings: AppConfig = load(
    ConfigFactory.parseString(secrets)
  )

  // Read the B64 encoded string and dump contents to a zip file.
  // The datastax cassandra connector needs a zip file with the
  // connection package. Return the path to the zip file.
  private def createDbConnectionPackage(): String = {
    // CAUTION: verify the file name matches the cloud.path in the
    // spark session init config values.
    Properties.envOrNone("DB_SECRETS_B64") match {
      case Some(b64Contents) =>
        val zipPath = "/tmp/datastax-db-secrets.zip"
        val os      = new FileOutputStream(zipPath)
        os.write(Base64.getDecoder.decode(b64Contents).toArray)
        os.close()

        log.info(s"Created datastax cassandra connection package from B64 at $zipPath")
        zipPath
      case _ =>
        // in dev/test. use existing secrets file.
        log.debug("Using local DB connection bundle.")
        "/home/dev/work/datastax-db-secrets.zip"
    }
  }

  private def getPostgresConfig(config: Config): PostgresConfig = {
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

  /** Load from a given Typesafe Config object */
  def load(config: Config): AppConfig = {
    val runtime: String = Properties.envOrElse("RUNTIME_ENV", "development")

    AppConfig(
      database = DatastaxConfig(
        url = config.getString("secrets.database.datastaxAstra.url"),
        appToken = config.getString("secrets.database.datastaxAstra.token"),
        clientId = config.getString("secrets.database.datastaxAstra.clientID"),
        clientSecret = config.getString("secrets.database.datastaxAstra.clientSecret"),
        connectionPackagePath = createDbConnectionPackage()
      ),
      runtimeEnv = runtime,
      inProd = if (runtime.equals("docker")) true else false,
      redis = extractRedisConfig(config),
      sampleDfs = if (Properties.envOrNone("SAMPLE_DFS").isEmpty) false else true,
      timezone = Properties.envOrElse("TZ", "America/Los_Angeles"),
      pg = getPostgresConfig(config)
    )
  }
}
