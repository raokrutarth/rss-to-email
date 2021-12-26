# rss-to-email

Simple server that keeps checking the configured RSS feeds and
sends periodic emails to the configured destination address.

## TODOs

- configuration and secrets `.template` files.
- Setup sendgrid free tier.
- Setup secrets read.
- docker compose.
- sql-lite & volume mount.
  - last access offset per resource.
- APS scheduler.

## Ideas

- NLTK tag nouns and verbs.
  - Stem and lem verbs.
  - same document/email = linked.
  - Use graphx library to find nouns linked 2/3 edges away.
  - Use syn-nets to generalize verbs for better links.
