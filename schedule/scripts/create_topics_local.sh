#!/bin/bash

# Create a set of 6 PCF kafka-topic-services from command line to be used by veraseda

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <broker> [<partitions>]"
  echo " e.g. $0 localhost:9092"
  echo " will create these topics:"
  echo " - \"accepted\""
  echo " - \"scheduled\""
  echo " - \"paused\""
  echo " - \"failed\""
  echo " - \"completed\""
  echo " - \"notified\""
  exit 1
fi

if [ "$KAFKA" == "" ]; then
  echo "Define Kafka installation \$KAFKA first! E.g.:"
  echo "export KAFKA=\"D:/tools/kafka/kafka_2.13-2.8.0\""
  exit 1
fi

if [[ "$OS" == "Windows_NT" ]]; then
  KAFKA_TOPICS=$KAFKA/bin/windows/kafka-topics.bat
  KAFKA_CONFIGS=$KAFKA/bin/windows/kafka-configs.bat
else
  # KAFKA_TOPICS=$KAFKA/bin/kafka-topics.sh
  # KAFKA_CONFIGS=$KAFKA/bin/kafka-configs.bat
  KAFKA_TOPICS="podman exec -it kafka-1 kafka-topics"
  KAFKA_CONFIGS="podman exec -it kafka-1 kafka-configs"
fi

broker="${1}"
partitions="${2:-6}"

for topic in accepted scheduledA01 scheduledA02 scheduledA03 pausedB01 pausedB02 failed completed; do
  set -x
  $KAFKA_TOPICS --bootstrap-server "$broker" --create --if-not-exists --topic "$topic" --partitions $partitions --replication-factor 1
  $KAFKA_TOPICS --bootstrap-server "$broker" --create --if-not-exists --topic "${topic}.dlq" --partitions $partitions --replication-factor 1
  $KAFKA_TOPICS --bootstrap-server "$broker" --create --if-not-exists --topic "${topic}.err" --partitions $partitions --replication-factor 1
  set +x
done

for topic in paused notified; do
  set -x
  $KAFKA_TOPICS --bootstrap-server "$broker" --create --if-not-exists --topic "$topic" --partitions $partitions --replication-factor 1
  set +x
done


