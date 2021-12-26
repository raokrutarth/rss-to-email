package fyi.newssnips.webapp.config

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Properties
import java.util.Base64
import scala.io.Source
import com.typesafe.scalalogging.Logger
import fyi.newssnips.shared.config.SharedConfig

// https://github.com/alexandru/scala-best-practices/blob/master/sections/
/* 3-architecture.md#35-must-not-use-parameterless-configfactoryload-or-access-a-config-object-directly */

case class AppConfig(
    comms: CommsConfig,
    hostName: String, // localhost/website address
    shared: SharedConfig
)

case class CommsConfig(
    sendgridApiKey: String
)

object AppConfig {
  // System.setProperty("logback.configurationFile", "/home/dev/work/webapp/conf/logback.xml")
  val log: Logger = Logger("app." + this.getClass().toString())

  // no way to mount files in cloud so convert the
  // secrets to base64 string and decode from env var.
  private val secrets: String = Properties.envOrNone("SECRETS_B64") match {
    case Some(b64Contents) =>
      // Running in heroku. read b64 encoded secrets file.
      log.info("Using secrets provided from env vars.")
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

  /** Load from a given Typesafe Config object */
  def load(config: Config): AppConfig = {
    AppConfig(
      hostName =
        if (SharedConfig.config.inProd) "https://newssnips.fyi" else "http://localhost:9000",
      shared = SharedConfig.config,
      comms = CommsConfig(config.getString("email.sendgrid.apiKey"))
    )
  }
}
