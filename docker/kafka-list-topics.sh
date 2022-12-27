#!/bin/bash

docker exec -it ${1:-kafka-1} kafka-topics \
    --bootstrap-server ${1:-kafka-1}:${2:-9092} \
    --list

# kafka-topics --describe --topic job-accepted --bootstrap-server ${1:-kafka-1}:${2:-9092}
