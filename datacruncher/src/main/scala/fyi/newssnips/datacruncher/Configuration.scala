package configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.File
import org.apache.http.client.config.RequestConfig
import java.io.FileOutputStream
import com.typesafe.scalalogging.Logger
import java.util.Base64
import java.nio.file.Files
import java.nio.file.Paths

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
/* 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly */

case class AppConfig(
    database: DatastaxConfig,
    sendgrid: SendgridConfig,
    runtimeEnv: String,
    inProd: Boolean,
    httpClientConfig: RequestConfig,
    redis: RedisConfig,
    pg: PostgresConfig,

    // timezone of app from
    /* https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/ZoneId.html */
    timezone: String,

    // when set via env var, intermediate dataframes
    // are sampled during cycle for debugging.
    sampleDfs: Boolean,
    modelsPath: String,
    newsApi: NewsApiConfig
)

case class PostgresConfig(
    connStr: String
)

case class NewsApiConfig(
    apiKey: String
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

case class SendgridConfig(
    apiKey: String
)

/* https://stackoverflow.com/questions/20879639/write-base64-encoded-image-to-file */
object AppConfig {
  val log: Logger = Logger("app." + this.getClass().toString())

  val settings: AppConfig = load(
    ConfigFactory.parseFile(
      new File(
        // can specify the config file path via env var or the
        // secrets.conf file in the current directory is used.
        Properties.envOrElse("SECRETS_FILE_PATH", "secrets.conf")
      )
    )
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

        log.info(
          s"Created datastax cassandra connection package from B64 at $zipPath"
        )
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

    RedisConfig(
      url = connUrl,
      host = host,
      port = port,
      password = password,
      useTls = false
    )
  }

  private def getPostgresConfig(config: Config): PostgresConfig = {
    val user     = config.getString("secrets.database.yugabyte.user")
    val password = config.getString("secrets.database.yugabyte.password")
    val host     = config.getString("secrets.database.yugabyte.host")
    val port     = config.getString("secrets.database.yugabyte.port")
    val certPath = "/home/dev/work/yugabyte_db_cert.crt"
    val db       = "newssnips"

    PostgresConfig(
      s"jdbc:postgresql://${host}:${port}" +
        s"/${db}?user=${user}&password=${password}&ssl=true&sslmode=verify-full&sslrootcert=${certPath}"
    )
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
        clientSecret =
          config.getString("secrets.database.datastaxAstra.clientSecret"),
        connectionPackagePath = createDbConnectionPackage()
      ),
      sendgrid = SendgridConfig(
        apiKey = config.getString("secrets.sendgrid.apiKey")
      ),
      newsApi = NewsApiConfig(
        config.getString("secrets.data_sources.news_api.api_key")
      ),
      pg = getPostgresConfig(config),
      runtimeEnv = runtime,
      inProd = if (runtime.equals("docker")) {
        log.warn("Running in production mode.")
        true
      } else false,
      httpClientConfig = requestConfig,
      timezone = Properties.envOrElse("TZ", "America/Los_Angeles"),
      redis = extractRedisConfig(config),
      sampleDfs =
        if (Properties.envOrNone("SAMPLE_DFS").isEmpty) false else true,
      modelsPath = {
        val dirInContainer = "/etc/models"
        if (Files.isDirectory(Paths.get(dirInContainer))) {
          dirInContainer
        } else "models"
      }
    )
  }
}
