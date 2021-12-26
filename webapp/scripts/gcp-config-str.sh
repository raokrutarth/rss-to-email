#!/bin/bash -e

SECRETS_PATH=${1:-"webapp/secrets.conf"}
DB_SECRETS_PATH=${2:-"datastax-db-secrets.zip"}

printf "SECRETS_B64=$(base64 -w0 ${SECRETS_PATH}),DB_SECRETS_B64=$(base64 -w0 ${DB_SECRETS_PATH})"
