#!/bin/bash -ex

BUILD_TAG=${1:-"newssnips-datacruncher:dev"}

# run the build command to create the
# executable in the dev-env container
if [[ ! -v no_compile ]]; then
    docker exec rss-dev \
        bash -c \
        "pushd /home/dev/work/datacruncher && /home/dev/.local/share/coursier/bin/sbt Docker/stage"
fi

cp -r datacruncher/scripts datacruncher/target/docker/stage

docker build \
    -t "${BUILD_TAG}" \
    -f deploy/Dockerfile-dc \
    datacruncher/target/docker/stage

if [[ -v RUN ]]; then
    # test the image locally if needed.

    docker rm -f rss-dc-test || true
    docker run --rm -it \
    --name rss-dc-test \
    -e SECRETS_FILE_PATH=/etc/secrets.conf \
    -v "/home/zee/sharp/rss-to-email/datacruncher/secrets.conf":/etc/secrets.conf:ro \
    -e SHARED_SECRETS_FILE_PATH=/etc/shared.secrets.conf \
    -v "/home/zee/sharp/rss-to-email/shared.secrets.conf":/etc/shared.secrets.conf:ro \
    -e PG_CERT_PATH=/etc/pg.crt \
    -v "/home/zee/sharp/rss-to-email/cockroachdb_db.crt":/etc/pg.crt:ro \
    -v "/home/zee/sharp/rss-to-email/datacruncher/models":/etc/models:ro \
    "${BUILD_TAG}"
fi
