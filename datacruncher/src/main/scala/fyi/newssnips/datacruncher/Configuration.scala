package configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.File
import org.apache.http.client.config.RequestConfig
import java.io.FileOutputStream
import com.typesafe.scalalogging.Logger
import java.util.Base64

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
/* 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly */

case class AppConfig(
    database: DatastaxConfig,
    sendgrid: SendgridConfig,
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

case class SendgridConfig(
    apiKey: String
)

/* https://stackoverflow.com/questions/20879639/write-base64-encoded-image-to-file */
object AppConfig {
  val log: Logger = Logger(this.getClass())

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
      runtimeEnv = runtime,
      inProd = if (runtime.equals("docker")) true else false,
      httpClientConfig = requestConfig
    )
  }
}
