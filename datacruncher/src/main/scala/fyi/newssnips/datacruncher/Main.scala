package fyi.newssnips.datacruncher

import fyi.newssnips.datacruncher.scripts._
import com.typesafe.scalalogging.Logger

object Main extends App {
  val log = Logger("app." + this.getClass().toString())

  val usage = "$ [expriment|cycle|scratch]"
  log.info(s"Running datacruncher with args: ${this.args.mkString(", ")}")
  val mode = args(0)

  mode match {
    case "expriment" => ModelExpriments.sentiment()
    case "cycle"     =>
      // https://flurdy.com/docs/scalainit/startscala.html
      AnalysisCycle
    case "scratch"     => ScratchCode
    case "model_store" => ModelStore.storeCycle()
    case _ =>
      log.error(s"$mode is an invalid mode.")
      sys.exit(1)
  }

  log.info("datacruncher execution finished.")
}
