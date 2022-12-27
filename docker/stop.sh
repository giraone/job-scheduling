#!/bin/bash

docker-compose down
sleep 10
./data-cleanup.sh
