#!/bin/bash

docker rm receiver
docker rmi receiver
cd ../receiver
mvn clean
mvn -ntp verify -DskipTests jib:dockerBuild
cd -

docker rm materialize
docker rmi materialize
cd ../materialize
mvn clean
mvn -ntp verify -DskipTests jib:dockerBuild
cd -

docker rm schedule
docker rmi schedule
cd ../schedule
mvn clean
mvn -ntp verify -DskipTests jib:dockerBuild
cd -

docker rmi jobadmin
cd ../jobadmin
mvn -ntp verify -DskipTests -Pprod jib:dockerBuild
cd -
