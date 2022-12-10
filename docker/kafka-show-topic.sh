#!/bin/bash

docker exec -it kafka-1 bash -c "kafka-console-consumer \
  --bootstrap-server kafka-1:9092 \
  --property print.key=true \
  --property print.partition=true \
  --topic ${1:-job-accepted} \
  --from-beginning"
