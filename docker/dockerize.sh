#!/bin/bash

cd receiver
mvn -ntp verify -DskipTests -Pprod jib:dockerBuild
cd -

cd materialize
mvn -ntp verify -DskipTests -Pprod jib:dockerBuild
cd -

cd schedule
mvn -ntp verify -DskipTests -Pprod jib:dockerBuild
cd -

cd jobadmin
mvn -ntp verify -DskipTests -Pprod jib:dockerBuild
cd -