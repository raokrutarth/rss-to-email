#!/bin/bash -ex

BUILD_TAG=${1:-"newssnips-webapp-dev:$(date +%m-%d-%Y)"}

# run the build command to create the
# executable in the dev-env container
docker exec rss-dev \
    bash -c "pushd /home/dev/work/webapp && /home/dev/.local/share/coursier/bin/sbt Docker/stage"

docker build \
    -t "${BUILD_TAG}" \
    -f deploy/Dockerfile-wa \
    webapp/target/docker/stage

if [[ -v RUN ]]; then
    # test the image locally if needed.
    # FIXME need to disable minikube when mounting local files
    # test local with:  curl -X GET "$(minikube ip)":9001/rss/report/24h
    docker run \
    --name rss-test \
    --rm \
    -it \
    --env SECRETS_B64="$(base64 -w0 webapp/secrets.conf)" \
    --env YB_CERT_B64="$(base64 -w0 yugabyte_db_cert.crt)" \
    "${BUILD_TAG}"
fi
