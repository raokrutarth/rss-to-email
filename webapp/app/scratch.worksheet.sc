import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import fyi.newssnips.shared.config.SharedConfig
import com.typesafe.scalalogging.Logger
import java.lang.Runtime

 DateTimeFormatter.ofPattern("E, d LLL YYYY").format(
     OffsetDateTime.now(ZoneId.of("UTC"))
 )