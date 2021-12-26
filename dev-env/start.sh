#!/bin/bash -ex

# Script to start the development container which has all
# the development utilities installed.

# run from repo root directory

docker stop scala-dev && docker rm scala-dev || true

docker build --file dev-env/Dockerfile -t scala-dev-img ./dev-env

docker run \
    -d \
    -p 34000:3000 \
    -p 34022:22 \
    -e JUPYTER_ENABLE_LAB=no \
    -e GRANT_SUDO=yes \
    --restart "unless-stopped" \
    -v "${PWD}":/home/jovyan/dev \
    --name scala-dev \
    scala-dev-img

if [[ -v RUN ]]; then
    docker exec -it scala-dev bash
fi
