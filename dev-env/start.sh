#!/bin/bash -ex

# Script to start the development container which has all
# the development utilities installed.

# NOTE run from repo root directory

docker rm -f rss-dev || true

if [[ -v BUILD ]]; then
	docker build \
	--file dev-env/Dockerfile \
	--tag rss-dev-img:v2 ./dev-env
fi

docker run \
  -d \
  --restart "unless-stopped" \
  -v "${PWD}":/home/dev/work \
  --name rss-dev \
  rss-dev-img:v2

docker update \
  --memory=8Gi \
  --cpus=4 \
  rss-dev

if [[ -v RUN ]]; then
  docker exec -it rss-dev bash
fi
