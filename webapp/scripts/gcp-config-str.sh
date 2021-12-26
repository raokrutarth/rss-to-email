#!/bin/bash -e
echo "SECRETS_B64=$(base64 -w0 ./secrets.conf),DB_SECRETS_B64=$(base64 -w0 ../datastax-db-secrets.zip)"
