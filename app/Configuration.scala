package configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.File

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
// 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly

case class AppConfig(
    database: DatastaxConfig,
    sendgrid: SendgridConfig,
    runtimeEnv: String,
    inProd: Boolean
)

case class DatastaxConfig(
    url: String,
    appToken: String
)

case class SendgridConfig(
    apiKey: String
)

object AppConfig {

  val settings: AppConfig = load(
    ConfigFactory.parseFile(
      new File(
        // can specify the config file path via env var or the
        // secrets.conf file in the current directory is used.
        Properties.envOrElse("SECRETS_FILE_PATH", "secrets.conf")
      )
    )
  )

  /** Load from a given Typesafe Config object */
  def load(config: Config): AppConfig = {
    val runtime: String = Properties.envOrElse("RUNTIME_ENV", "development")
    AppConfig(
      database = DatastaxConfig(
        url = config.getString("secrets.database.datastaxAstra.url"),
        appToken = config.getString("secrets.database.datastaxAstra.token")
      ),
      sendgrid = SendgridConfig(
        apiKey = config.getString("secrets.sendgrid.apiKey")
      ),
      runtimeEnv = runtime,
      inProd = if (runtime.equals("docker")) true else false
    )
  }
}
