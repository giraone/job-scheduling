#!/bin/bash

./data-cleanup.sh
./data-setup.sh
docker-compose up -d
sleep 30
./kafka-create-topics.sh
