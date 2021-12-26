import com.johnsnowlabs.nlp.pretrained.ResourceDownloader

// ResourceDownloader.showPublicPipelines(lang = "en")

import scala.xml.Utility.unescape

val sb = new StringBuilder
val s =
  "Gottlieb: Potential vaccine mandate for young children &#039;a long way off&#039;"

unescape(s, sb)
