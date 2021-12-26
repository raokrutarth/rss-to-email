package fyi.newssnips.datacruncher

import com.typesafe.scalalogging.Logger

object PerformanceStats {
  val log = Logger("app." + this.getClass().toString())

  def logMemory() = {
    // memory info
    val mb      = 1024 * 1024
    val runtime = Runtime.getRuntime
    log.info(
      "** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb + " MB"
    )
    log.info("** Max Memory:   " + runtime.maxMemory / mb)
  }
}
