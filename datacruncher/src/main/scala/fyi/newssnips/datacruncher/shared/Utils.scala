package fyi.newssnips.shared

// import org.apache.spark.sql._
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import configuration.AppConfig
import com.typesafe.scalalogging.Logger
import java.lang.Runtime

// object DfUtils {
//   private val log: Logger = Logger("app." + this.getClass().toString())

//   /** Log n random values from the dataframe if the envorinment variable for df sampeling is
//     * enabled. Can reduce performance.
//     */
//   def showSample(
//       df: DataFrame,
//       n: Float = 5f,
//       truncate: Int = 30, // num chars
//       overrideEnv: Boolean = false
//   ): Unit = {
//     if (AppConfig.settings.sampleDfs || overrideEnv) {
//       log.info(s"Sampling dataframe with ${df.count()} rows.")
//       df.sample(n / math.max(n, df.count())).show(20, truncate = truncate)
//     }
//   }
// }

object PerformanceUtils {
  private val log: Logger = Logger("app." + this.getClass().toString())

  val mb = 1024 * 1024
  def logMem() = {
    val runtime = Runtime.getRuntime
    log.info("Memory usage: " + (runtime.totalMemory - runtime.freeMemory) / mb)
    log.info("Free memory: " + runtime.freeMemory / mb)
  }
}

// wrapper around date time utils so
// library used can be changed as needed.
object DateTimeUtils {
  private val tz = ZoneId.of(AppConfig.settings.timezone)
  private val displayDateFormatter =
    DateTimeFormatter
      .ofLocalizedDateTime(FormatStyle.MEDIUM)
      .withZone(tz)

  def now(): OffsetDateTime =
    OffsetDateTime.now(tz)

  def getDateAsString(d: OffsetDateTime): String = {
    displayDateFormatter.format(d)
  }

  def convertStringToDate(s: String): OffsetDateTime = {
    OffsetDateTime.parse(s, displayDateFormatter)
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
