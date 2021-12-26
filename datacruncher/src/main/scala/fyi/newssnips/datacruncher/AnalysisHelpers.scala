package fyi.newssnips.datacruncher

import org.apache.spark.sql.functions._

object NerHelper {
  def normalizeEntityName(en: String): String = {
    en match {
      case "US" | "U.S" | "U.S.A" | "USA" | "United States of America" =>
        "United States"
      case en if (en.startsWith("the ") || en.startsWith("The ")) =>
        normalizeEntityName(
          en.replaceFirst("(The\\s*|the\\s*)", "")
        )
      case en if (en.endsWith("'s")) => en.slice(0, en.lastIndexOfSlice("'s"))
      case _                         => en
    }
  }

  val entityNameNormalizeUdf = udf((x: String) => normalizeEntityName(x))
}
