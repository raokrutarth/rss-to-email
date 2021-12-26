#!/bin/bash -e

echo "Set these config vars in heroku."

echo "SECRETS_B64="
base64 -w0 ./secrets.conf

printf "\n\n"

echo "DB_SECRETS_B64="
base64 -w0 ../datastax-db-secrets.zip
echo 
