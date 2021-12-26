#!/bin/bash -ex

export JAVA_OPTS="-Xms1024M -Xmx7048M -XX:+UseG1GC -XX:+UseStringDeduplication"
export TF_CPP_MIN_LOG_LEVEL=2
export RUNTIME_ENV=docker

/usr/bin/time -v sbt "run cycle"
