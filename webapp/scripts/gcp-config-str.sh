#!/bin/bash -e

# prints out the comma seperated env vars that 
# need to be set in the gcp config update command.

SECRETS_PATH="webapp/secrets.conf"
PG_CERT_PATH="cockroachdb_db.crt"
SHARED_SECRETS_PATH="shared.secrets.conf"

printf "SECRETS_B64=$(base64 -w0 ${SECRETS_PATH}),"
printf "SHARED_SECRETS_B64=$(base64 -w0 ${SHARED_SECRETS_PATH}),"
printf "PG_CERT_B64=$(base64 -w0 ${PG_CERT_PATH})"
