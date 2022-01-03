package fyi.newssnips.webapp.core.books
import play.twirl.api.Html
import com.typesafe.scalalogging.Logger
import scala.util.Random

object Books {

  private val log: Logger = Logger("app." + this.getClass().toString())
  private val books = Map(
    "GPE" -> Seq( //"Countries, cities, states.",
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0805078533&asins=0805078533&linkId=8012ef06f07a8280de736abee2ddca96&show_border=false&link_opens_in_new_window=true"></iframe>
      """,
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1541675819&asins=1541675819&linkId=bc78a5fd91ea1e9d9c3f80f96b87c0e9&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=059331817X&asins=059331817X&linkId=45392f493fbba4b08e0c155a1f2ed736&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1982168439&asins=1982168439&linkId=235bdd63047a204b45e2032322d9da7a&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // extreme economies
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1784163252&asins=1784163252&linkId=5e829cfed1beb284690cfb2f5affca00&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // narrow corridor
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07MCRLV2K&asins=B07MCRLV2K&linkId=48fa5485f10bbff0723bf9233797496e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // why nations fail
      """
        <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0307719227&asins=0307719227&linkId=68a56834c83766b650cd043664663d0c&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // nudge
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B097NJJ4PY&asins=B097NJJ4PY&linkId=ae961f5bdaa615fa250f3f6c2ad0416e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // seeing like a state
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0300078153&asins=0300078153&linkId=e0c9cb0483f1dd522f74ffb4742d61d0&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // good econ hard times
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07ZWKHM8N&asins=B07ZWKHM8N&linkId=329236a7753ac1acc8019cc8e9e88c8e&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ),
    "PERSON" -> Seq(
      // models of the mind
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B092GF4WTT&asins=B092GF4WTT&linkId=00bafe50759da72c8bd6347adcc4a7df&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // range
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0735214506&asins=0735214506&linkId=a2a630b0a5d81813067a7c0a600a0e5e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // sweet spot
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0062910566&asins=0062910566&linkId=de6f0b12437157adfd008e02af14156b&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1524796468&asins=1524796468&linkId=0036cddbefbc81fe59e2b14c0c9f7bd8&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // road to character
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B00V3WMYN0&asins=B00V3WMYN0&linkId=237adf453eeccece47a9facabb06acd7&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // willpower
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07QZT49MK&asins=B07QZT49MK&linkId=4828e96cccec8d95a096e193b592d1d8&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ),
    "NORP" -> Seq(
      // useful delusions
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0393652203&asins=0393652203&linkId=3d3205a779436882d48eae60141eab74&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // talking to strangers
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0316299227&asins=0316299227&linkId=fc74111eca43418bd2b9a498c475f575&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // power of us
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0316538418&asins=0316538418&linkId=f2de97a0c3b9393c3f0bc6f76ab27860&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // social chemistry
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1524743801&asins=1524743801&linkId=6a3e2d00e13789a8bdbeeb6c22dd93ee&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // critical thinking
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0262538288&asins=0262538288&linkId=b94e678caac9911c267d4db7aaf61e53&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // strategy of conflict
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0674840313&asins=0674840313&linkId=12939abc95e4f1d54fa49967c45abdbd&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1473693667&asins=1473693667&linkId=f41a5bb51e66c0bbdbf1924a9c697cf5&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // madness of the crowds
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1539849589&asins=1539849589&linkId=ba3a42ece425841cfa04bd7f5c2cb34e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // thinking fast and slow
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0374275637&asins=0374275637&linkId=d7b3b2618fb66ee2026d8ff26427bb6e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // not born yesterday
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0691208921&asins=0691208921&linkId=c9da138c7ac0d5ae1639283469b4c32b&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // moral tribes
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B00GBE63LE&asins=B00GBE63LE&linkId=8c64f333e7e1a11eaff97871d3fb5b0b&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ), //"Nationalities or religious or political groups.",
    "FAC" -> Seq(), //"Buildings, airports, highways, bridges, etc.",
    "ORG" -> Seq(
      // winner take all
      """
    <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07FNSVK5S&asins=B07FNSVK5S&linkId=26e908e13e6655e2aea0c93acefa4883&show_border=true&link_opens_in_new_window=true"></iframe>
    """,
      // superforecasting
      """
    <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0804136718&asins=0804136718&linkId=3ccccd3fdf8866c6ee006756c7a880e3&show_border=true&link_opens_in_new_window=true"></iframe>
    """,
      // architects of intellegence
      """
    <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B0812B9X5G&asins=B0812B9X5G&linkId=1e5e7d28b649a1868038251af7601970&show_border=true&link_opens_in_new_window=true"></iframe>
    """,
      // sand hill road
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=059308358X&asins=059308358X&linkId=4e07b7206fc8825c9793f10ac670bd5e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // recommendation engines
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0262539071&asins=0262539071&linkId=0db65e04dcec0efbc30800010fc75f5c&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // positioning
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0071373586&asins=0071373586&linkId=ac765b7900557ff322946dba3c6b2370&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // attention merchants
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B01M19JRIO&asins=B01M19JRIO&linkId=0bf170521c753462c89ab63545f61efd&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ), //"Companies, agencies, institutions, etc.",
    "LOC" -> Seq(
      // deep survival
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B00G09UL22&asins=B00G09UL22&linkId=1e799250fd63f5ea6af79be2b0582826&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ), //"Non-GPE locations, mountain ranges, bodies of water.",
    "PRODUCT" -> Seq(
      // enough
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0470524235&asins=0470524235&linkId=d293c0966f495165b03e1393f3a21ffb&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // sand hill road
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=059308358X&asins=059308358X&linkId=4e07b7206fc8825c9793f10ac670bd5e&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // recommendation engines
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0262539071&asins=0262539071&linkId=0db65e04dcec0efbc30800010fc75f5c&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // positioning
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0071373586&asins=0071373586&linkId=ac765b7900557ff322946dba3c6b2370&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // attention merchants
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B01M19JRIO&asins=B01M19JRIO&linkId=0bf170521c753462c89ab63545f61efd&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // who gets what
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B00WZXD3CM&asins=B00WZXD3CM&linkId=229035d04f7319e8192e1cdb34c8d12c&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // power of expriments
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0262542277&asins=0262542277&linkId=88ecb87ec0acaf99c3d8530c60a1219d&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // new new thing
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B001H071HC&asins=B001H071HC&linkId=63ce478ca97fc81d89e7efb08c7d1557&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ), //"Objects, vehicles, foods, etc. (Not services.)",
    "EVENT" -> Seq(
      // art of rhetoric
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0140445102&asins=0140445102&linkId=594b8a081cca093ccdd6ec0db488896c&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // im lying
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07RK54H63&asins=B07RK54H63&linkId=5b2fda68ba93a9173e242160ade3b4b0&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // deep survival
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B00G09UL22&asins=B00G09UL22&linkId=1e799250fd63f5ea6af79be2b0582826&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // narraative economics
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07VZWLRM8&asins=B07VZWLRM8&linkId=d1e1c1f01079184560e4e353568a5ed3&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // made to stick
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B000MGBNM6&asins=B000MGBNM6&linkId=f166bed86b2c259fda2ed2b099d7478f&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ), //"Named hurricanes, battles, wars, sports events, etc.",
    "WORK_OF_ART" -> Seq(), //"Titles of books, songs, etc.",
    "LAW" -> Seq(
      // power of us
      """
    <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0316538418&asins=0316538418&linkId=f2de97a0c3b9393c3f0bc6f76ab27860&show_border=true&link_opens_in_new_window=true"></iframe>
    """
    ), //"Named documents made into laws.",
    "LANGUAGE" -> Seq(
      // language instinct
      """
    <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0061336467&asins=0061336467&linkId=6b65cecb82be60c7f84908421131d292&show_border=true&link_opens_in_new_window=true"></iframe>
    """,
      // how the mind works
      """
    <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0393334775&asins=0393334775&linkId=8e64334b87f7118496f54d82baa35af1&show_border=true&link_opens_in_new_window=true"></iframe>
    """
    ), //"Any named language.",
    "MONEY" -> Seq(
      // rentier capitalism
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=1788739728&asins=1788739728&linkId=bd1a699a5b0aa2c8e828631abfc32132&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // dao of capital
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B08DH78T9N&asins=B08DH78T9N&linkId=c75cb23170087eafc10630598a8d6d05&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // man who solved the market
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=073521798X&asins=073521798X&linkId=ff67931e806e83575aa4006b4dc4d97b&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // enough
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0470524235&asins=0470524235&linkId=d293c0966f495165b03e1393f3a21ffb&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
      // unknown market wizards
      """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0857198696&asins=0857198696&linkId=13c2b952ca27b56521b0cea144bd5789&show_border=true&link_opens_in_new_window=true"></iframe>
      """
    ), //"Monetary values, including unit.",
    "QUANTITY" -> Seq() //"Measurements, as of weight or distance.",
  )

  private val negativeBooks = Seq(
    // critical thinking
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0262538288&asins=0262538288&linkId=b94e678caac9911c267d4db7aaf61e53&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // im lying
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07RK54H63&asins=B07RK54H63&linkId=5b2fda68ba93a9173e242160ade3b4b0&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // art of rhetoric
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0140445102&asins=0140445102&linkId=594b8a081cca093ccdd6ec0db488896c&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // strategy of conflict
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=0674840313&asins=0674840313&linkId=12939abc95e4f1d54fa49967c45abdbd&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // bumliminal mind
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B007WZU3E4&asins=B007WZU3E4&linkId=926d30773a8722e19a57bfa6cd285e9d&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // everybody lies
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B06XCYD5KG&asins=B06XCYD5KG&linkId=1496e92b1fba04f19604db9ebb73c38a&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // flah boys
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B00ICRE1QC&asins=B00ICRE1QC&linkId=1e63a6865b843f90f765ab0595a3b2a3&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // algos of opression
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B07CX7ZVZT&asins=B07CX7ZVZT&linkId=6f40a9fc746a2937d58b102bf8513b6f&show_border=true&link_opens_in_new_window=true"></iframe>
      """,
    // makers takers
    """
      <iframe style="width:120px;height:240px;" marginwidth="0" marginheight="0" scrolling="no" frameborder="0" src="//ws-na.amazon-adsystem.com/widgets/q?ServiceVersion=20070822&OneJS=1&Operation=GetAdHtml&MarketPlace=US&source=ss&ref=as_ss_li_til&ad_type=product_link&tracking_id=sentipeg-20&language=en_US&marketplace=amazon&region=US&placement=B01CUKFLII&asins=B01CUKFLII&linkId=9b76d3f9fe06f2f85048d43700d6afca&show_border=true&link_opens_in_new_window=true"></iframe>
      """
  )

  def getBooks(entityType: String, sentiment: String): Seq[Html] = {
    log.info(s"Getting book recommendations for entity $entityType and sentiment $sentiment.")

    if (sentiment.toLowerCase().equals("negative")) {
      Random.shuffle(negativeBooks.map(s => Html(s))).take(3)
    } else {
      val iframes = books.getOrElse(entityType, Seq())
      Random.shuffle(iframes.map(s => Html(s))).take(3)
    }

  }
}
