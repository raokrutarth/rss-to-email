#!/bin/bash -ex

# sbt Docker/stage
# /usr/bin/time -v target/docker/stage/opt/docker/bin/newssnips-webapp

sbt stage
PORT=9000 /usr/bin/time -v \
  target/universal/stage/bin/newssnips-webapp -Dhttps.port=9000
