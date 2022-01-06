#!/bin/bash -ex
# dev utility to kill zombie development server process.

# TODO not finding pid
pid=`sudo lsof -n -i :9000 | grep LISTEN`

if [[ ! -z "$pid" ]]; then 
  echo $pid
fi
