#!/bin/bash -ex

BUILD_TAG=${1:-"newssnips-datacruncher-dev"}

# run the build command to create the
# executable in the dev-env container
# docker exec rss-dev \
#     bash -c "pushd /home/dev/work/datacruncher && /home/dev/.local/share/coursier/bin/sbt Docker/stage"

# point docker to minikube for registry-free image transfer
# if [[ ! -v RUN ]]; then
#     eval $(minikube -p minikube docker-env)
# fi

docker build \
    -t "${BUILD_TAG}" \
    -f deploy/Dockerfile \
    --build-arg BINARY_FILE=/opt/docker/bin/datacruncher \
    datacruncher/target/docker/stage

if [[ -v RUN ]]; then
    # test the image locally if needed.
    # FIXME need to disable minikube when mounting local files
    # test local with:  curl -X GET "$(minikube ip)":9001/rss/report/24h
    docker rm -f rss-dc-test || true
    docker run \
    --name rss-dc-test \
    -d \
    -e DB_SECRETS_B64="$(base64 -w0 /home/zee/sharp/rss-to-email/datastax-db-secrets.zip)" \
    -e SECRETS_FILE_PATH=/etc/secrets.conf \
    -v "/home/zee/sharp/rss-to-email/datacruncher/secrets.conf":/etc/secrets.conf:ro \
    "${BUILD_TAG}"

    docker update rss-dc-test --memory 8Gi --cpus 6
    docker logs -f rss-dc-test
fi

# if [[ ! -v RUN ]]; then
#     eval $(minikube -p minikube docker-env -u)
# fi