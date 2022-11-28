#!/bin/bash -e

# prints out the comma seperated env vars that
# need to be set in the the IaaS config update command.

SECRETS_PATH="webapp/secrets.conf"
PG_CERT_PATH="cockroachdb_db.crt"
SHARED_SECRETS_PATH="shared.secrets.conf"

if [[ -v gcp ]]; then
  # for gcp
  printf "SECRETS_B64=$(base64 -w0 ${SECRETS_PATH}),"
  printf "SHARED_SECRETS_B64=$(base64 -w0 ${SHARED_SECRETS_PATH}),"
  printf "PG_CERT_B64=$(base64 -w0 ${PG_CERT_PATH})"
elif [[ -v heroku ]]; then
  # for heroku
  printf "SECRETS_B64=$(base64 -w0 ${SECRETS_PATH}) "
  printf "SHARED_SECRETS_B64=$(base64 -w0 ${SHARED_SECRETS_PATH}) "
  printf "PG_CERT_B64=$(base64 -w0 ${PG_CERT_PATH})"
else
  # for koyeb
  printf "%s" "--env SECRETS_B64=$(base64 -w0 ${SECRETS_PATH}) "
  printf "%s" "--env SHARED_SECRETS_B64=$(base64 -w0 ${SHARED_SECRETS_PATH}) "
  printf "%s" "--env PG_CERT_B64=$(base64 -w0 ${PG_CERT_PATH})"
  printf "%s" "--env PORT=8000"
fi
