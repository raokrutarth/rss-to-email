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

### Resources

- Scala talks (see other years) <https://scaladays.org/2019/lausanne/schedule>
- linting <https://www.youtube.com/watch?v=E06VRtUfdVM>
- <https://github.com/search?l=Scala&o=desc&p=2&q=scala+play+framework&s=updated&type=Repositories>
- <https://github.com/Tusharrajbhardwaj/HR_RestApi_CRUD_Scala_ReactiveMongo_PlayFramework>
- Slick an mySql <https://github.com/MaxPsm/CRUD_test>
- example web scraping app <https://github.com/userOT/Forex>
- sample play projects: <https://developer.lightbend.com/start/?group=play>
- Directory level linting: <https://github.com/japgolly/scala-restructure>
