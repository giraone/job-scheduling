#!/bin/bash

for topic in job-accepted job-accepted-err \
             job-paused-B01 job-paused-B02 job-paused-err \
             job-scheduled-A01 job-scheduled-A02 job-scheduled-A03 job-scheduled-err \
	     job-failed-A01 job-failed-A02 job-failed-A03 job-failed-err \
	     job-completed job-completed-err \
	     job-notified job-notified-err \
	     job-delivered; do

  echo "Create $topic"
  docker exec -it kafka-1 kafka-topics \
    --bootstrap-server kafka-1:9092 \
    --create \
    --topic $topic \
    --replication-factor 1 \
    --partitions 8
done

# kafka-configs --alter --topic job-accepted --add-config "cleanup.policy=compact" --add-config "delete.retention.ms=10000" --bootstrap-server kafka-1:9092
# kafka-topics --describe --topic job-accepted --bootstrap-server kafka-1:9092
