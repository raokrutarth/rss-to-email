#!/bin/bash -ex

ADDR="localhost:9000"

curl -X POST \
  -H "Content-Type: application/json" \
  -d @feeds.json ${ADDR}/v1/rss/report