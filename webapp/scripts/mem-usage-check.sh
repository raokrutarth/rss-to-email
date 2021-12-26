#!/bin/bash -ex

sbt Docker/stage
/usr/bin/time -v target/docker/stage/opt/docker/bin/newssnips-webapp
