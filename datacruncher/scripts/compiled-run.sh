#!/bin/bash -ex

export JAVA_OPTS="-Xms1024M -Xmx7048M -XX:+UseG1GC -XX:+UseStringDeduplication"
export TF_CPP_MIN_LOG_LEVEL=2
# export RUNTIME_ENV=docker

set +x
export DB_SECRETS_B64="$(base64 -w0 /home/dev/work/datastax-db-secrets.zip)"
set -x
export SECRETS_FILE_PATH="/home/dev/work/datacruncher/secrets.conf"
export MODELS_DIR="/home/dev/work/datacruncher/models"

if [[ ! -v no_build ]]; then
    sbt "Docker/stage"
fi

mkdir -p ./prod-run
cp -r target/docker/stage/2/opt ./prod-run
cp -r target/docker/stage/4/opt ./prod-run

pushd prod-run/opt/docker
/usr/bin/time -v bin/datacruncher cycle

# cleanup
function finish {
  popd
  rm -rf ./prod-run
}
trap finish EXIT

