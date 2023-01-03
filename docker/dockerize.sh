#!/bin/bash

docker stop receiver
docker rm receiver
docker rmi receiver
cd ../receiver
mvn -ntp verify -DskipTests jib:dockerBuild
cd -

docker stop materialize
docker rm materialize
docker rmi materialize
cd ../materialize
mvn -ntp verify -DskipTests jib:dockerBuild
cd -

docker stop schedule
docker rm schedule
docker rmi schedule
cd ../schedule
mvn -ntp verify -DskipTests jib:dockerBuild
cd -

docker stop jobadmin
docker rm jobadmin
docker rmi jobadmin
cd ../jobadmin
mvn -ntp verify -DskipTests -Pprod jib:dockerBuild
cd -
