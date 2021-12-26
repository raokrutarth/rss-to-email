#!/bin/bash -ex

echo "Run build with command in dev-env: sbt docker:stage"

eval $(minikube -p minikube docker-env)

docker build \
    -t rss-to-email:latest \
    -f deploy/Dockerfile \
    target/docker/stage

if [[ -v RUN ]]; then
    dummy_token="SL0kKTecNCH3Q_hT2r79bzkc3i7myMkxcnmoNuJAAB8"

    docker run \
    --rm \
    -it \
    -p 9001:9000 \
    rss-to-email:latest
fi
eval $(minikube -p minikube docker-env -u)