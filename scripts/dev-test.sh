#!/bin/bash -ex

ADDR="localhost:9000"
FEED_FILE="./app/resources/feeds-news.json"

# curl -X POST \
#   -H "Content-Type: application/json" \
#   -d @"${FEED_FILE}" \
#   ${ADDR}/v1/rss/report


curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"urls":["http://feeds.marketwatch.com/marketwatch/topstories/"]}' \
  ${ADDR}/v1/rss/report