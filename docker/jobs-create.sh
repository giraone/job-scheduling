#!/bin/bash

if [[ $# != 1 ]]; then
  echo "Usage: $0 <amount-of-jobs>"
  echo "  e.g. $0 10"
  exit 1
fi

typeset -i amount=$1
processKey=${2:-A01}
topic=job-accepted

while (( i < amount )); do

  id=$(date +%s)
  echo "${id}|{\"id\":${id},\"processKey\":\"${processKey}\", \"eventTimestamp\":\"$(date --iso-8601=seconds)\"}" | \
  docker exec -i kafka-1 kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --property parse.key=true \
    --property key.separator=\| \
    --topic ${topic}

  echo -n '.'
  (( i+=1 ))
done

echo
