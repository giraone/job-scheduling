#!/bin/bash

docker exec -it ${2:-kafka-1} bash -c "kafka-console-consumer \
  --bootstrap-server ${2:-kafka-1}:${3:-9092} \
  --property print.key=true \
  --property print.partition=true \
  --topic ${1:-job-accepted} \
  --from-beginning"
