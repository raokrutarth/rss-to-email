package configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.FileOutputStream
import org.apache.http.client.config.RequestConfig
import java.util.Base64
import scala.io.Source
import play.api.Logger

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
/* 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly */

case class AppConfig(
    database: DatastaxConfig,
    redis: RedisConfig,
    runtimeEnv: String,
    inProd: Boolean,
    httpClientConfig: RequestConfig
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
        "/home/dev/work/datastax-db-secrets.zip"
    }
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

    val timeout = 10
    val requestConfig = RequestConfig
      .custom()
      .setConnectTimeout(timeout * 1000)
      .setConnectionRequestTimeout(timeout * 1000)
      .setSocketTimeout(timeout * 1000)
      .build()

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
      httpClientConfig = requestConfig,
      redis = extractRedisConfig(config)
    )
  }
}
