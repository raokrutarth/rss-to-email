package configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.io.File
import org.apache.http.client.config.RequestConfig
import com.typesafe.scalalogging.Logger
import java.nio.file.Files
import java.nio.file.Paths
import fyi.newssnips.shared.config._

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
/* 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly */

case class AppConfig(
    httpClientConfig: RequestConfig,
    // when set via env var, intermediate dataframes
    // are sampled during cycle for debugging.
    sampleDfs: Boolean,
    modelsPath: String,
    newsApi: NewsApiConfig,
    shared: SharedConfig
)

case class NewsApiConfig(
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

  /** Load from a given Typesafe Config object */
  def load(config: Config): AppConfig = {

    val timeout = 10
    val requestConfig = RequestConfig
      .custom()
      .setConnectTimeout(timeout * 1000)
      .setConnectionRequestTimeout(timeout * 1000)
      .setSocketTimeout(timeout * 1000)
      .build()

    AppConfig(
      newsApi = NewsApiConfig(
        config.getString("data_sources.news_api.api_key")
      ),
      httpClientConfig = requestConfig,
      sampleDfs =
        if (Properties.envOrNone("SAMPLE_DFS").isEmpty) false else true,
      modelsPath = {
        val dirInContainer = "/etc/models"
        if (Files.isDirectory(Paths.get(dirInContainer))) {
          dirInContainer
        } else Properties.envOrElse("MODELS_DIR", "models")
      },
      shared = SharedConfig.config
    )
  }
}
