package fyi.newssnips.datastore

// https://cassandra.apache.org/doc/latest/cassandra/getting_started/drivers.html
// .builder()
//       .withCloudSecureConnectBundle(Paths.get(AppConfig.settings.database.connectionPackagePath))
//       .withAuthCredentials(
//         AppConfig.settings.database.clientId,
//         AppConfig.settings.database.clientSecret
//       )
//       .build()
// https://github.com/outworkers/phantom/issues/933
// https://blog.knoldus.com/getting-started-phantom/
// https://outworkers.github.io/phantom/basics/database.html
// https://outworkers.github.io/phantom/basics/connectors.html

import configuration.AppConfig

import java.nio.file.Paths
// import com.datastax.driver.core.Cluster
import com.datastax.oss.driver.api.core.CqlSession

class ConnectDatabase() {
  val session = CqlSession
    .builder()
    .withCloudSecureConnectBundle(
      Paths.get(AppConfig.settings.database.connectionPackagePath)
    )
    .withAuthCredentials(
      AppConfig.settings.database.clientId,
      AppConfig.settings.database.clientSecret
    )
    .withKeyspace("dev")
    .build()

  def test() = {
    // todo use for simpler queries
    println(session.execute("select * from home_page_analysis_results"))
    session.close()
  }
}
