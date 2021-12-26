#!/bin/bash -ex

ADDR="localhost:9000"

# add feed
# curl -X POST ${ADDR}/rss/url \
#   -d '{"url":"https://www.nasdaq.com/feed/rssoutbound"}' \
#   -H "Content-Type: application/json" 

# list feeds
curl -X GET ${ADDR}/rss/urls

# trigger report
# curl -X GET ${ADDR}/rss/report/24h

# delete feed