#!/bin/bash

docker exec -it kafka-1 kafka-topics \
    --bootstrap-server kafka-1:9092 \
    --list

# kafka-topics --describe --topic job-accepted --bootstrap-server kafka-1:9092
