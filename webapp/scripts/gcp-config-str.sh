#!/bin/bash -e

SECRETS_PATH=${1:-"webapp/secrets.conf"}
DB_CERT_PATH=${2:-"yugabyte_db_cert.crt"}

printf "SECRETS_B64=$(base64 -w0 ${SECRETS_PATH}),YB_CERT_B64=$(base64 -w0 ${DB_CERT_PATH})"
