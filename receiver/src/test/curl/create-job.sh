#!/bin/bash

if [[ $# != 1 ]]; then
  echo "Usage: $0 @<json-file>|<json-data>"
  exit 1
fi

data="$1"

curl http://localhost:8090/api/jobs \
     --request POST \
     --silent \
     --header 'Content-Type: application/json' \
     --data "$data"
echo