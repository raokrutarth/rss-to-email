package fyi.newssnips.datacruncher.utils

import org.apache.spark.sql._
import configuration.AppConfig
import com.typesafe.scalalogging.Logger

object DfUtils {
  private val log: Logger = Logger("app." + this.getClass().toString())

  /** Log n random values from the dataframe if the envorinment variable for df
    * sampeling is enabled. Can reduce performance.
    */
  def showSample(
      df: DataFrame,
      n: Float = 5f,
      truncate: Int = 30, // num chars
      overrideEnv: Boolean = false
  ): Unit = {
    if (AppConfig.settings.sampleDfs || overrideEnv) {
      log.info(s"Sampling dataframe with ${df.count()} rows.")
      df.sample(n / math.max(n, df.count())).show(20, truncate = truncate)
    }
  }
}
