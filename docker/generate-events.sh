#!/bin/bash

if (( $# != 2 )); then
  echo "Usage $0 <id> accepted|scheduled|failed|completed"
  echo " e.g. $0 10 accepted"
  exit 1
fi

id="$1"
status="$2"
if [[ "$status" == "accepted" ]]; then
  topic="job-accepted"
else
  topic="job-update"
fi

docker exec -i kafka-1 bash -c "echo \"${id}|{\\\"id\\\":${id},\\\"status\\\":\\\"${status}\\\"}\" | kafka-console-producer \
  --bootstrap-server kafka-1:9092 \
  --property parse.key=true \
  --property key.separator=\| \
  --topic ${topic}"
