package fyi.newssnips.datacruncher

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql._
import fyi.newssnips.models.FeedURL
import fyi.newssnips.datacruncher.core.Scraper

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import fyi.newssnips.shared.DbConstants
import fyi.newssnips.shared.DateTimeUtils

import scala.util.Failure
import fyi.newssnips.datacruncher.core.Analysis

import fyi.newssnips.models.Feed
import fyi.newssnips.datastore.Cache
import org.apache.spark.storage.StorageLevel
import fyi.newssnips.datacruncher.datastore.SparkPostgres
import fyi.newssnips.datacruncher.scripts.ModelExpriments
import fyi.newssnips.datacruncher.scripts.ModelStore
import configuration.AppConfig

object AnalysisCycle {
  private val log = Logger("app." + this.getClass().toString())

  lazy val spark: SparkSession =
    SparkSession
      .builder()
      .appName("newssnips.fyi")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .config("spark.sql.broadcastTimeout", 1200)
      .master("local[*]")
      .getOrCreate()

  import spark.implicits._

  private val db = new SparkPostgres(spark)

  private val cache = new Cache()

  private val analysis = new Analysis(spark)
  log.info("Initalized analysis module.")

  private val dataPrep = new DataPrep(spark)
  log.info("Initalized dataprep module.")

  private val booksFinderUdf = udf(ContextFinder.findBooks)

  val categoryToUrls: Map[String, Seq[String]] = Map(
    "home" -> Seq(
      "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
      "https://www.reddit.com/r/UpliftingNews/.rss",
      "https://www.reddit.com/r/worldnews/.rss",
      "http://rss.cnn.com/rss/cnn_world.rss",
      "http://rss.cnn.com/rss/cnn_us.rss",
      "http://rss.cnn.com/rss/cnn_latest.rss",
      "https://nypost.com/feed",
      "http://feeds.feedburner.com/zerohedge/feed",
      "https://www.huffpost.com/section/front-page/feed?x=1",
      "http://feeds.foxnews.com/foxnews/latest",
      "http://feeds.foxnews.com/foxnews/world",
      "https://cdn.feedcontrol.net/8/1114-wioSIX3uu8MEj.xml",
      "https://www.yahoo.com/news/rss",
      "https://www.rt.com/rss/news/",
      "https://www.investing.com/rss/news.rss",
      "https://api.axios.com/feed/",
      "https://www.washingtontimes.com/rss/headlines/news/",
      "https://www.washingtontimes.com/rss/headlines/news/world/",
      "https://slate.com/feeds/all.rss",
      "https://www.ft.com/world?format=rss",
      "https://www.ft.com/home-beta?format=rss",
      "https://www.chicagotribune.com/arcio/rss/category/news/breaking/",
      "https://www.aljazeera.com/xml/rss/all.xml",
      "https://cms.qz.com/feed/"
    ),
    "markets" -> Seq(
      "http://feeds.marketwatch.com/marketwatch/realtimeheadlines/",
      "https://www.ft.com/markets?format=rss",
      "https://slate.com/feeds/business.rss",
      "https://finance.yahoo.com/news/rss",
      "https://www.nasdaq.com/feed/rssoutbound",
      "https://seekingalpha.com/market_currents.xml",
      "https://seekingalpha.com/feed.xml",
      "http://feeds.feedburner.com/TradingVolatility",
      "http://rss.politico.com/economy.xml",
      "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
      "http://thehill.com/taxonomy/term/30/feed",
      "http://thehill.com/taxonomy/term/20/feed",
      "https://nypost.com/business/feed/",
      "http://feeds.marketwatch.com/marketwatch/topstories/",
      "https://www.investing.com/rss/investing_news.rss",
      "https://www.investing.com/rss/stock.rss",
      "https://www.washingtonexaminer.com/tag/economy.rss",
      "https://www.washingtontimes.com/rss/headlines/news/business-economy/",
      "http://feeds.benzinga.com/benzinga/best-of-benzinga",
      "https://www.benzinga.com/top-stories/feed",
      "https://www.benzinga.com/economics/feed",
      "https://www.marketscreener.com/rss/FeedNews.php",

      // https://tradingeconomics.com/rss/ (see other useful)
      // "https://tradingeconomics.com/rss/news.aspx", (generic headline for every country. e.g. GDP)
      // "https://tradingeconomics.com/united-states/rss",

      // "https://fool.libsyn.com/rss",
      "https://www.cnbc.com/id/20409666/device/rss/rss.html?x=1",
      "https://www.wallstreetsurvivor.com/feed/",
      "https://www.investing.com/rss/news_25.rss",
      "https://www.reddit.com/r/StockMarket/.rss",
      "https://www.reddit.com/r/wallstreetbets/.rss",
      "https://www.reddit.com/r/stocks/.rss"
    ),
    // "technology" -> Seq("https://www.techmeme.com/feed.xml"),
    "politics" -> Seq(
      "https://rss.politico.com/congress.xml",
      "http://thehill.com/rss/syndicator/19110",
      "https://www.memeorandum.com/feed.xml",
      "https://www.reddit.com/r/politics/.rss",
      "https://www.reddit.com/r/NeutralPolitics/.rss",
      "https://news.yahoo.com/rss/politics",
      "https://www.washingtonexaminer.com/tag/politics.rss",
      "https://www.nationalreview.com/feed/",
      "https://fivethirtyeight.com/politics/feed/",
      "https://www.politifact.com/rss/all/",
      "https://www.politifact.com/rss/factchecks/",
      "https://www.washingtontimes.com/rss/headlines/news/politics/",
      "https://slate.com/feeds/news-and-politics.rss",
      "https://www.rollingstone.com/politics/feed/",
      "https://www.chicagotribune.com/arcio/rss/category/politics/",
      "http://feeds.feedburner.com/realclearpolitics/qlMj",
      "https://www.benzinga.com/topic/government/feed"
    ),
    "entertainment" -> Seq(
      "https://www.buzzfeed.com/celebrity.xml",
      "https://www.buzzfeed.com/tvandmovies.xml",
      "http://syndication.eonline.com/syndication/feeds/rssfeeds/topstories.xml",
      "https://meredith.mediaroom.com/news-releases?pagetemplate=rss&category=816",
      "http://feeds.bet.com/AllBetcom",
      "https://www.hollywoodintoto.com/feed/",
      "https://hollywoodlife.com/feed/",
      "https://mtonews.com/.rss/full/",
      "http://feeds.feedburner.com/variety/headlines",
      "https://www.reddit.com/r/celebrities/.rss",
      "https://www.reddit.com/r/entertainment/.rss",
      "https://www.wesmirch.com/feed.xml",
      "https://mediagazer.com/feed.xml",
      "https://www.yahoo.com/entertainment/rss",
      "https://www.rollingstone.com/music/feed/",
      "https://www.rollingstone.com/tv/feed/",
      "https://www.rollingstone.com/movies/feed/",
      "https://www.chicagotribune.com/arcio/rss/category/entertainment/",
      "https://www.hollywoodreporter.com/t/awards/feed/",
      "https://www.hollywoodreporter.com/c/news/feed/",
      "https://www.hollywoodreporter.com/c/movies/feed/",
      "https://www.hollywoodreporter.com/c/tv/feed/",
      "https://www.hollywoodreporter.com/t/international/feed/"
    )
  )

  for ((categoryId, urls) <- categoryToUrls) {
    log.info(
      s"Generating feeds for category $categoryId with ${urls.size} URL(s)."
    )
    val categoryMetadata = DbConstants.categoryToDbMetadata(categoryId)

    // se only one feed in developemnt/test mode
    val cycleUrls =
      if (AppConfig.settings.shared.inProd) urls.distinct
      else urls.distinct.take(2)

    val categoryFeeds: Seq[Feed] = cycleUrls.flatMap { u =>
      // TODO group urls by host/first8-chars and reduce sleep time.
      Thread.sleep(726) // sleep to avoid rate limiting
      Scraper.getAndParseFeed(FeedURL(u))
    }

    // create feeds df
    val feedsDf: DataFrame = categoryFeeds.zipWithIndex
      .map { case (f, i) =>
        (i, f.title, f.url.value, DateTimeUtils.getDateAsString(f.lastScraped))
      }
      .toDF("feed_id", "title", "url", "last_scraped")

    log.info(s"Feeds for category ${categoryId}:")
    feedsDf.show(false) // small table. ok to print.

    val categoryContents = categoryFeeds.map { f => f.content }.flatten

    val fatContentsDf       = dataPrep.constructContentsDf(categoryContents)
    val (urlDf, contentsDf) = dataPrep.seperateLinksFromContents(fatContentsDf)

    // metadata if the report generation is successful. (df, tableName, idCol)
    val toSave: Seq[(DataFrame, String)] = Seq(
      (feedsDf, categoryMetadata.sourceFeedsTableName),
      (urlDf, categoryMetadata.articleUrlsTableName),
      (contentsDf, categoryMetadata.textsTableName)
    )
    contentsDf.persist(StorageLevel.DISK_ONLY)

    val reportDf = analysis.generateReport(contentsDf)

    reportDf match {
      case Some(df) =>
        log.info("Fetching context products for all entities in report.")

        val resDf = df
          .withColumn(
            "contextBooks",
            booksFinderUdf(col("entityName"), col("entityType"))
          )

        log.info(s"Final analysis report for $categoryId:")
        resDf.persist(StorageLevel.DISK_ONLY)
        resDf.show() // critical dataframe to see before saving

        db.putDataframe(
          categoryMetadata.analysisTableName,
          resDf
        ) match {
          case Failure(s) =>
            log.error(s"Failed to store analysis. Reason: $s")
          case _ =>
            resDf.unpersist()
            log.info(
              s"${categoryMetadata.analysisTableName} rows saved successfully. Saving analysis metadata."
            )
            toSave.foreach { case (df, tableName) =>
              db.putDataframe(tableName, df) match {
                case Failure(exception) =>
                  log.error(
                    s"Failed to save table $tableName with error $exception"
                  )
                case _ =>
                  log.info(s"Saved metadata table $tableName")
              }
            }
            db.upsertKV(
              categoryMetadata.lastUpdateKey,
              DateTimeUtils.getDateAsString(DateTimeUtils.now())
            ) match {
              case Failure(_) =>
                log.error(
                  s"Failed to set updated-at time for ${categoryMetadata.lastUpdateKey}"
                )
              case _ =>
                log.info(
                  s"$categoryId page analysis and metadata successfully saved."
                )
            }
        }
      case _ => log.error("Unable to generate home page results report.")
    }
    contentsDf.unpersist()
  }
  cache.flushCache()
  cache.cleanup()
  spark.stop()
}

object ScratchCode {
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("newssnips.fyi")
      .config(
        "spark.serializer",
        "org.apache.spark.serializer.KryoSerializer"
      )
      .master("local[*]")
      .getOrCreate()

  import spark.implicits._

  val origDf = Seq(
    ("U.S.", "abc1", 1),
    ("U.S", "abc99", 1),
    ("General Electric", "abc2", 1),
    ("GE", "abc3", 1),
    ("the U.S.", "abc4", 1),
    ("US", "abc5", 1),
    ("Unites States", "iio", 6)
  ).toDF("entityName", "entityType", "count")

  origDf.show(false)

  origDf
    .withColumn(
      "entityName",
      NerHelper.entityNameNormalizeUdf(col("entityName"))
    )
    .show(false)

  spark.stop
}

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
