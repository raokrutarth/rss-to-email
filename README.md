# rss-to-email

A personal project intended to scrape rss feeds and send email. Turned into a mission-driven project to allow people find positive news.

## Design Decisions

- Stateless containerized data pipeline and webapp for horizontal scalability.
- Spark + Scala + SparkNLP based data pipeline for sentiment analysis and NER.
- Scala + Play based web app hosted in a free-tier cloud provider.
- Postgress and Redis for persistant store.

### Supported Entity Types

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
