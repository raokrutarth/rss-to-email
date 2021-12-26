# rss-to-email

Simple server that keeps checking the configured RSS feeds and
sends periodic emails to the configured destination email address.

## Usage

TODO

## Design

TODO: k8s, minikube, docker images, scale, apache spark, batch/stream.

## Development

Secton covers items and resources used during development.

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