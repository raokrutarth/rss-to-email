#!/bin/bash -ex

export JAVA_OPTS="-Xms1024M -Xmx6048M -XX:+UseG1GC"
export TF_CPP_MIN_LOG_LEVEL=2

/usr/bin/time -v sbt run
