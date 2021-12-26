#!/bin/bash -ex

BUILD_TAG=${1:-"newssnips-datacruncher:$(date +%m-%d-%Y)"}

# run the build command to create the
# executable in the dev-env container
docker exec rss-dev \
    bash -c "pushd /home/dev/work/datacruncher && /home/dev/.local/share/coursier/bin/sbt Docker/stage"

# point docker to minikube for registry-free image transfer
# if [[ ! -v RUN ]]; then
#     eval $(minikube -p minikube docker-env)
# fi

docker build \
    -t "${BUILD_TAG}" \
    -f deploy/Dockerfile \
    target/docker/stage

if [[ -v RUN ]]; then
    # test the image locally if needed.
    # FIXME need to disable minikube when mounting local files
    # test local with:  curl -X GET "$(minikube ip)":9001/rss/report/24h
    docker run \
    --name rss-test \
    --rm \
    -it \
    -p 9001:9001 \
    -e PORT=9001 \
    -e SECRETS_FILE_PATH=/etc/secrets.conf \
    --mount type=bind,source="$(pwd)/secrets.conf",target=/etc/secrets.conf \
    "${BUILD_TAG}"
fi

echo "${BUILD_TAG}"

# if [[ ! -v RUN ]]; then
#     eval $(minikube -p minikube docker-env -u)
# fi