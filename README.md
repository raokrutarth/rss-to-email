# rss-to-email

Simple server that keeps checking the configured RSS feeds and
sends periodic emails to the configured destination email address.

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
- Similar APIs:
  - <https://rapidapi.com/twinword/api/twinword-text-analysis-bundle/details>
  - <https://rapidapi.com/blog/sentiment-analysis-apis/>
- ML module design idea: <https://www.kaggle.com/general/202189>
- Top rss feed finder <https://eztoolset.com/>