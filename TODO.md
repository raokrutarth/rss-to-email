# TODO

Wishlist of features and ideas to explore.

## Ideas

- NLTK tag nouns and verbs.
  - Stem and lem verbs.
  - same document/email = linked.
  - Use graphx library to find nouns linked 2/3 edges away.
  - Use syn-nets to generalize verbs for better links.

## both

- music background tutorial video/gif.
- launch checklist.
  - reddit & blog post.
  - print flyers in gym & downtown streets.
- Create Instagram, reddit and facebook account.

## wa

- newsletter welcome and update email.
- multi-table home page.
- entity search functionality.
- cache clear on deploy.

## dc

- verify time for stale article.
- twitter bot for daily post. (WIP)
- collect feeds that did not work at each cycle.
  - may need automated admin notification feature.
- title cleanup.
  - google news nested titles with html read.
- move to headline only + description use presentation style.
  - see how use use descriptions for sentiment without showing them.

## non-urgent

- Automation:
  - rss feed URL finder script using nav bar links.
- Call cache warm after update.
- Hardcoded books links for each category.
- Look into @samanthasunne and toolsforreporters website.
- Add admin dashboard.
- paginated analysis page view.
- Precomputing texts for top x topics in each category and add to cache.
- add context column.
- add "for businesses" section. e.g. filtering feeds.
- move to bootstrap 5.1
- enable pagination for text and entity pages.
- add historical data to s3/r2 for API.
- footer copyright & newsletter policy.
- scrape entrypoint router for xml vs REST scrape.
  - Add news API to scraper. (not a lot of data, needs scraper router).

### Resources

- hardcoded books:
  - <https://www.thewayoutofpolarization.com/buy-the-book>
  - narrow corridors
  - why nations fail
  - the everything store
  - the power of us
  - very important people
  - little no body - anna k
  - financial books
- Scala talks (see other years) <https://scaladays.org/2019/lausanne/schedule>
- Scala learning checklist <https://scalac.io/blog/scala-isnt-hard-how-to-master-scala-step-by-step/>
- linting <https://www.youtube.com/watch?v=E06VRtUfdVM>
- <https://github.com/search?l=Scala&o=desc&p=2&q=scala+play+framework&s=updated&type=Repositories>
- Directory level linting: <https://github.com/japgolly/scala-restructure>
- best practices: <https://github.com/alexandru/scala-best-practices/blob/master/sections/3-architecture.md>
- Scala reference projects
  - <https://github.com/guardian/prism>
  - <https://github.com/guardian/frontend-email-reporting>
  - <https://developer.lightbend.com/start/?group=play>
  - web scraping: <https://github.com/userOT/Forex>
  - Slick an mySql <https://github.com/MaxPsm/CRUD_test>
  - <https://github.com/Tusharrajbhardwaj/HR_RestApi_CRUD_Scala_ReactiveMongo_PlayFramework>
- DB connection performance: <https://www.playframework.com/documentation/2.1.0/ThreadPools>
- cassandra datastax ORM <https://github.com/DataStax-Examples/object-mapper-jvm>
- <https://github.com/lauris/awesome-scala>
- Feeds to explore:
  - <https://rss.com/blog/popular-rss-feeds/>
  - <https://www.feedspot.com/?continue=brandmonitoring>
- Scala/Java NLP librarier: <https://www.predictiveanalyticstoday.com/top-free-software-for-text-analysis-text-mining-text-analytics/>
- monitoring <https://medium.com/i-love-my-local-farmer-engineering-blog/monitoring-serverless-java-applications-b0f15c487364>
- proxy apis <https://rapidapi.com/collection/proxy>
- basic session, login and authentication in code: <https://pedrorijo.com/blog/scala-play-auth/>
- mySQL on k8s cluster
  - <https://github.com/kubernetes/examples/tree/master/staging/storage/mysql-galera>
  - <https://jekhokie.github.io/ubuntu/linux/python/docker/container/kubernetes/minikube/2018/09/05/kubernetes-part-2-python-flask-application-deployment.html>
  - <https://kubernetes.io/docs/tutorials/stateful-application/mysql-wordpress-persistent-volume/#visit-your-new-wordpress-blog>
- report ideas (see DS blogs and sites (kaggle)):
  - <https://www.luminoso.com/>
  - <https://www.lexalytics.com/ssv>
  - <https://blog.hubspot.com/service/sentiment-analysis-tools>
- Similar: <https://mailbrew.com/>
- affiliate website book:
  - <https://english.api.rakuten.net/raygorodskij/api/GoogleBooks>
  - <https://english.api.rakuten.net/raygorodskij/api/Goodreads>
- account based:
  - send user-key and recovery key to email address.
  - if recovery key is used, assign new key and replace key in DB.
redis: <https://levelup.gitconnected.com/dockerizing-scala-redis-nginx-c97d067244d9>
spark docs: <https://spark.apache.org/docs/latest/api/scala/org/apache/spark/index.html>
scala + play tutorials: <https://www.youtube.com/watch?v=FqMDHsFNlxQ&list=PLLMXbkbDbVt8tBiGc1y69BZdG8at1D7ZF>
- Similar APIs:
  - <https://rapidapi.com/twinword/api/twinword-text-analysis-bundle/details>
  - <https://rapidapi.com/blog/sentiment-analysis-apis/>
- ML module design idea: <https://www.kaggle.com/general/202189>
- Top rss feed finder <https://eztoolset.com/>
- Slick + PG tutorial <https://sysgears.com/articles/how-to-create-restful-api-with-scala-play-silhouette-and-slick/>
- Report visualisation ideas <https://textvis.lnu.se/>
- PII removal with regex <https://medium.com/spark-nlp/cleaning-and-extracting-content-from-html-xml-documents-using-spark-nlp-documentnormalizer-913d96b2ee34>
- Twitter sentiment analysis <https://github.com/ReeceASharp/TwitterTweetScraper/blob/562f76691bac04f076bbae32b1825ea246ffabf8/src/main/scala/SparkNLP.scala>
- Scala + Kafka <https://www.youtube.com/watch?v=k_Y5ieFHGbs>
- Payment apis <https://www.entrepreneur.com/slideshow/300214>
- rss feeds lists:
  - <https://blog.feedspot.com/world_news_rss_feeds/>
  - <https://blog.feedspot.com/category/>
- Dashboard html templates: <https://flatlogic.com/blog/top-material-react-admin-dashboards/>
- dark theme UI toggle:
  - <https://codepen.io/ravencolevol/pen/NWWXaBB>
  - <https://codepen.io/j_holtslander/pen/MRbpLX>
  - search e.x. "bootstrap dark theme coderpen"
  - <https://medium.com/front-end-field-guide/automatic-dark-mode-for-your-website-b446d8a3b8a5>
- material design example apps:
  - <https://glitch.com/@material/material-studies/play/fortnightly>
  - <https://material.io/resources>
  - <https://glitch.com/@material/material-starter-kits>
  - <https://github.com/material-components/material-web>
  - With login <https://glitch.com/~material-example-app-shrine>

- example bootstrap page from scratch <https://www.w3schools.com/bootstrap/bootstrap_theme_company.asp>
- bootstrap dark theme css <https://bootstrap.themes.guide/darkster/>
- popover for links from buttons<https://getbootstrap.com/docs/4.0/components/popovers/>
- custom hybrid hosting solution:
  - <https://inlets.dev/> $20/m
  - <https://ngrok.com/pricing> $5/m
  - <https://alternativeto.net/software/inlets/>
- cheapest hosting solutions (2 core 4 GB ram ~$20/m):
  - <https://www.vultr.com/products/cloud-compute/>
  - <https://www.digitalocean.com/pricing/>
  - B2 plan <https://azure.microsoft.com/en-us/pricing/details/app-service/linux/#pricing>
  - e2 medium <https://cloud.google.com/run/pricing#cloud-run-pricing>
  - AWS ec2 t4g.medium HDD ~$15 <https://mtyurt.net/post/2021/why-we-chose-aws-ecs.html>
- spark project templates <https://github.com/foundweekends/giter8/wiki/giter8-templates>
- spark streaming code <https://medium.com/expedia-group-tech/apache-spark-structured-streaming-output-sinks-3-of-6-ed3247545fbc>
- datastax + spark integration.
- redis <https://faun.pub/how-to-share-spark-dataset-across-applications-using-redis-9f64e6e79352>
  - set eviction policy <https://flaviocopes.com/heroku-redis-maxmemory-policy/>
- spark advanced <https://medium.com/@vladimir.prus/advanced-custom-operators-in-spark-79b12da61ca7>
- ms how tos <https://docs.microsoft.com/en-us/azure/cosmos-db/cassandra/spark-upsert-operations>
- spark cassandra basics <https://itnext.io/spark-cassandra-all-you-need-to-know-tips-and-optimizations-d3810cc0bd4e>
- nice font and color scheme: <https://getbootstrap.com/docs/4.0/examples/blog/#>
- telegram bot <https://hackernoon.com/telegram-bot-in-scala-3-with-bot4s-http4s-doobie-for-ci-notifications>
- language models <https://www.reddit.com/r/LanguageTechnology/comments/lsckqc/train_multilingual_classifier_for_100_languages/>
- explore importing huggingface models to spark.
- async rest client within play <https://www.playframework.com/documentation/2.8.x/ScalaWS>
- 2gb ram <https://www.koyeb.com/pricing>
- percentile filter
  - `https://spark.apache.org/docs/3.1.1/api/python/reference/api/pyspark.sql.functions.percentile_approx.html`
- serverless container hosting:
  - `https://developers.cloudflare.com/load-balancing/additional-options/deploy-containerized-applications`
  - `https://docs.cloudfoundry.org/devguide/deploy-apps/push-docker.html`
  - `https://www.alibabacloud.com/product/swas`
  - `https://docs.microsoft.com/en-us/azure/container-instances/container-instances-quickstart`
  - `https://www.redhat.com/en/technologies/cloud-computing/openshift/container-platform`
  - `https://news.ycombinator.com/item?id=28841292`
  - `https://direktiv.io/`
- newsletter platforms:
  - sendgrid
    - `https://github.com/sendgrid/sendgrid-java/blob/main/USAGE.md#campaigns`
    - `https://docs.sendgrid.com/ui/sending-email/how-to-send-email-with-marketing-campaigns`
  - `https://zapier.com/blog/automate-email-newsletter-maintenance/`
  - `https://zapier.com/apps/categories/email-newsletters`
  - `https://mailchimp.com/pricing/free-details/`
  - `https://zapier.com/apps/mailchimp/integrations/webhook`
  - `https://www.mailerlite.com/pricing`
  - `https://substack.com/switch-to-substack` for features. Not API friendly.
  - `https://www.getrevue.co/` has API and twitter integration.
  - `https://www.zerobounce.net/email-validation-pricing` email validation.
  - `https://convertkit.com/pricing` good free tier. API support unknown.
  - `https://www.aweber.com/pricing.htm` same as above.
  - `https://www.sendinblue.com/pricing/`
  - `https://www.constantcontact.com/mainpage` has social ads built in.
  - `https://www.beehiiv.com/pricing` has email verification.
- basic user authentication:
  - `https://github.com/nulab/scala-oauth2-provider/`
  - `https://github.com/nulab/play2-oauth2-provider`
  - `https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Authentication`
  - `https://github.com/AlexITC/crypto-coin-alerts/blob/develop/alerts-server/app/com/alexitc/coinalerts/services/JWTAuthenticatorService.scala`
- rss URL finder
  - `http://www.aaronsw.com/2002/feedfinder/`
- rdb errors:
  - m [36mc.zaxxer.hikari.pool.ProxyConnection [0;39m [35m[0;39m HikariPool-1 - Connection org.postgresql.jdbc.PgConnection@53c37d2 marked as broken because of SQLSTATE(08006), ErrorCode(0)
- redis SSL via stunnel:
  - `https://morethantech.info/blogs/2020/04/26/redis-via-stunnel/`
- sendgrid bulk send: `https://www.twilio.com/blog/sending-bulk-emails-3-ways-sendgrid-nodejs`
- admin dashboard: `https://twitter.com/Cruip_com`
- blog site `https://chadbaldwin.net/2021/03/14/how-to-build-a-sql-blog.html`
- similar sites finders for dynamic rss feed crawlers:
  - `http://www.moreofit.com/similar-to/thehill.com/Top_10_Sites_Like_Thehill/?&page=2`
  - `https://www.similarsites.com/site/thehill.com`
- NER models to try:
  - `https://nlp.johnsnowlabs.com/2021/08/04/ner_ontonotes_roberta_base_en.html`
  - `https://nlp.johnsnowlabs.com/2021/08/04/ner_conll_roberta_base_en.html`
- seo optimization:
  - `https://www.crazyegg.com/` site scan
  - `https://matomo.org/` click tracking
  - `https://analytics.google.com/analytics/web/provision/#/provision`
- site & business status page
  - `https://instatus.com/blog`
- mobile app
  - `https://docs.flutter.dev/deployment/ios`
- marketing checklist start point"
  - `https://www.pinterest.com/business/hub`
  - `https://twitter.com/alexgarcia_atx/status/1463188550430699522`
  - `https://www.indiehackers.com/post/acquisition-channel-trends-opportunities-google-linkedin-pinterest-d621da3047`
  - `https://twitter.com/IndieHackers/status/1453320053999480832`
  - `https://blog.hubspot.com/blog/tabid/6307/bid/31147/the-ultimate-guide-to-mastering-pinterest-for-marketing.aspx`
  - `https://twitter.com/mynameis_davis/status/1473299500290695175?s=20`
  - `https://www.sendowl.com/where_to_sell`
  - `https://blog.howitzer.co/`
- slider `https://mdbootstrap.com/docs/b4/jquery/getting-started/installation/`
- saas boilerplate: <https://github.com/saasforge/open-source-saas-boilerpate>
