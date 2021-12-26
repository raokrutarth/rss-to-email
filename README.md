# rss-to-email

Simple server that keeps checking the configured RSS feeds and
sends periodic emails to the configured destination email address.

## TODO

- setup k8s worker cron job.
- setup heroku secrets and local dynamic secrets.
- makefile for deployments and management.

- use link in feedCOntent object to link to a given text.

## Usage

TODO

Entity recognition types:
```text
PERSON:      People, including fictional.
NORP:        Nationalities or religious or political groups.
FAC:         Buildings, airports, highways, bridges, etc.
ORG:         Companies, agencies, institutions, etc.
GPE:         Countries, cities, states.
LOC:         Non-GPE locations, mountain ranges, bodies of water.
PRODUCT:     Objects, vehicles, foods, etc. (Not services.)
EVENT:       Named hurricanes, battles, wars, sports events, etc.
WORK_OF_ART: Titles of books, songs, etc.
LAW:         Named documents made into laws.
LANGUAGE:    Any named language.
DATE:        Absolute or relative dates or periods.
TIME:        Times smaller than a day.
PERCENT:     Percentage, including ”%“.
MONEY:       Monetary values, including unit.
QUANTITY:    Measurements, as of weight or distance.
ORDINAL:     “first”, “second”, etc.
CARDINAL:    Numerals that do not fall under another type.
```

## Design

TODO: k8s, minikube, docker images, scale, apache spark, batch/stream.

## Development

Section covers items and resources used during development.

### Ideas

- NLTK tag nouns and verbs.
  - Stem and lem verbs.
  - same document/email = linked.
  - Use graphx library to find nouns linked 2/3 edges away.
  - Use syn-nets to generalize verbs for better links.

#### SaaS

Convert the application to a subscription based product with
annual fees.

- Find saas boilerplate: <https://github.com/saasforge/open-source-saas-boilerpate>

### Resources

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
redis: https://levelup.gitconnected.com/dockerizing-scala-redis-nginx-c97d067244d9
spark docs: https://spark.apache.org/docs/latest/api/scala/org/apache/spark/index.html
scala + play tutorials: https://www.youtube.com/watch?v=FqMDHsFNlxQ&list=PLLMXbkbDbVt8tBiGc1y69BZdG8at1D7ZF
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

names:
- sentimetrics
- news snack
- metricsfeed
- smetrics
- timelymetrics
- metrics-of-now
- buzzatory
- hype snack
- newsnips
- uptickmetrics
- news pulse
- report pulse
- feed pulse
- news net
- metrics buzz
- nautnews
- think news
- reporteer metrics
- daily metrics
- BUZZLYTICS.COM
- dalylitics
- METRICSLY
- news-keep
- news-report
- newsscreen
- feedscreen
- mainstream metrics
- informed.me
- mnmts
- feedish
- feed-slice
- feed-xray
- scoop-metrics
- newsmetry
- news-decrypt
- news-untangle
- decrypt-metrics
- sentiment-decrypt
- sentiment-inspect
- news inspect
- news elucidate
- news assort
- sentimen assort
- news guage
- news nick
- news appraise
- sentiment assay
- feed reckon
- sentiguage
- sentitally
- news docket
- senti-brief
- sentipeg
- news condense
- sentiment netshell
- news confab
- news chews
- news dissect
- scrutinize.news
- news intervene
