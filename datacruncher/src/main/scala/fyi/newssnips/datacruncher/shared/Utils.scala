package fyi.newssnips.shared

import org.apache.spark.sql._
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

object DfUtils {

  /* Log n random values from the dataframe */
  def showSample(
      df: DataFrame,
      n: Float = 5f,
      truncate: Boolean = true
  ): Unit = {
    df.sample(n / math.max(n, df.count())).show(truncate)
  }
}

object DateTimeUtils {
  private val displayDateFormatter = new SimpleDateFormat(
    "EEE, MMM dd, yyyy h:mm a z"
  )

  def now(): Date = Calendar.getInstance.getTime

  def getDateAsString(d: Date): String = {
    displayDateFormatter.format(d)
  }

  def convertStringToDate(s: String): Date = {
    displayDateFormatter.parse(s)
  }

  /* https://hussachai.medium.com/normalizing-a-date-string-in-the-scala-way-f37a2bdcc4b9 */
  /* https://www.java67.com/2019/01/10-examples-of-format-and-parse-dates-in-java.html */
//   def parseDate(s: String): Try[Date] = Try {

//     Try {DateUtils
//       .parseDateStrictly(
//         s,
//         Array(
//           "MM/dd/yyyy:hh:mm:ss",
//           "MM-dd-yyyy",
//           "MM/dd/yyyy",
//           "yyyy-MM-dd hh:mm:ss",
//           "yyyy-MM-dd",
//           "dd/MM/yyyy HH:mm:ss",
//           "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
//           "yyyy-MM-dd'T'HH:mm'Z'",
//           "M/d/yyyy h:mm:ss tt",
//           "ddd, MMM dd yyyy",
//           "dddd, MMMM dd, yyyy"
//         )
//       )
//     } match {
//       case Success(value) => value
//       case _ =>
//         val formatters = Seq(
//           DateTimeFormatter.ISO_OFFSET_DATE_TIME
//         )
//     }
//   }

//   def normalizeDate(dateStr: String): Option[String] = {
//   val trimmedDate = dateStr.trim
//   @tailrec
  /* def normalize(patterns: List[(String, DateTimeFormatter)]):
   * Try[TemporalAccessor] = patterns match { */
//     case head::tail => {
//       val resultTry = Try(head._2.parse(trimmedDate))
//       if(resultTry.isSuccess) resultTry else normalize(tail)
//     }
//     case _ => Failure(new RuntimeException("no match found"))
//   }
//   if(trimmedDate.isEmpty) None
//   else {
//     normalize(dateFormats).map(
//      iso8601DateFormatter.format(_)).toOption
//   }
// }
}
