#!/bin/bash -ex

# run the build command to create the
# executable in the dev-env container
docker exec rss-dev \
    bash -c "pushd /home/dev/work && /home/dev/.local/share/coursier/bin/sbt docker:stage"

# point docker to minikube for registry-free image transfer
eval $(minikube -p minikube docker-env)

docker build \
    -t rss-to-email:latest \
    -f deploy/Dockerfile \
    target/docker/stage

if [[ -v RUN ]]; then
    # test the image locally if needed.
    docker run \
    --rm \
    -it \
    -p 9001:9000 \
    rss-to-email:latest
fi
eval $(minikube -p minikube docker-env -u)