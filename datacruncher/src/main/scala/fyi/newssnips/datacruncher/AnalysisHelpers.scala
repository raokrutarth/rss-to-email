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
      case "Cdc"                => "CDC"
      case "Un" | "U.n"         => "United Nations"
      case "Nato"               => "North Atlantic Treaty Organization"
      case "Sec"                => "Securities and Exchange Commission"
      case "Irs"                => "Internal Revenue Service"
      case "Nfl"                => "NFL"
      case "Biden"              => "Joe Biden"
      case "Tesla" | "TSLA"     => "Tesla Inc"
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
      case _ => en
    }
  }

  val entityNameNormalizeUdf = udf((x: String) => normalizeEntityName(x))
}
