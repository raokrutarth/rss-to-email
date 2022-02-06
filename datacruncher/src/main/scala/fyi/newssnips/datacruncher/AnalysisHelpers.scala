package fyi.newssnips.datacruncher

import org.apache.spark.sql.functions._

object NerHelper {
  def normalizeEntityName(en: String): String = {
    // manual fix after case normalization is applied to entity name
    en match {
      case "Us" | "U.s" | "U.s.a" | "Usa" | "United States Of America" =>
        "United States"
      case "Uk" | "U.k"         => "United Kingdom"
      case "Eu" | "E.u"         => "European Union"
      case "Covid-19" | "Covid" => "COVID"
      case "Un" | "U.n"         => "United Nations"
      case "Nato"               => "North Atlantic Treaty Organization"
      case "Biden"              => "Joe Biden"
      case "Tesla" | "TSLA"     => "Tesla Inc"
      case "Russian"            => "Russia"
      case "Chinese"            => "China"
      case en if en.startsWith("The ") =>
        normalizeEntityName(
          en.replaceFirst("(The\\s*|the\\s*)", "")
        )
      case en if (en.endsWith("'s")) =>
        normalizeEntityName(
          en.slice(0, en.lastIndexOf("'s"))
        )
      case en if (en.endsWith("’s")) =>
        normalizeEntityName(
          en.slice(0, en.lastIndexOf("’s"))
        )
      case en if (en.length() <= 3) =>
        en.toUpperCase()
      case _ => en
    }
  }

  val entityNameNormalizeUdf = udf((x: String) => normalizeEntityName(x))
}
