#!/bin/bash -ex

ADDR="localhost:9000"

FEED="http://rss.cnn.com/rss/money_pf.rss"

# add feed
# curl -X POST ${ADDR}/rss/url \
#   -d '{"url":"'"${FEED}"'"}' \
#   -H "Content-Type: application/json" 

# # list feeds
# curl -X GET ${ADDR}/rss/urls

# trigger report
curl -X GET ${ADDR}/rss/report/24h

# # delete feed
# curl -X DELETE ${ADDR}/rss/url \
#   -d '{"url":"'"${FEED}"'"}' \
#   -H "Content-Type: application/json"