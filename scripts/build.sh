#!/bin/bash -ex

# run the build command to create the
# executable in the dev-env container
docker exec rss-dev \
    bash -c "pushd /home/dev/work && /home/dev/.local/share/coursier/bin/sbt Docker/stage"

# point docker to minikube for registry-free image transfer
if [[ ! -v RUN ]]; then
    eval $(minikube -p minikube docker-env)
fi

docker build \
    -t rss-to-email:latest \
    -f deploy/Dockerfile \
    target/docker/stage

if [[ -v RUN ]]; then
    # test the image locally if needed.
    # FIXME need to disable minikube when mounting local files
    # test local with:  curl -X GET "$(minikube ip)":9001/rss/report/24h
    docker run \
    --rm \
    -it \
    -p 9001:9000 \
    -e SECRETS_FILE_PATH=/etc/secrets.conf \
    --mount type=bind,source="$(pwd)/secrets.conf",target=/etc/secrets.conf \
    rss-to-email:latest
fi

if [[ ! -v RUN ]]; then
    eval $(minikube -p minikube docker-env -u)
fi