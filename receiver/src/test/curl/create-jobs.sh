#!/bin/bash

if [[ $# != 2 ]]; then
  echo "Usage: $0 <from> <to>"
  echo " e.g. $0 1000 2000"
  exit 1
fi

typeset -i from=$1
typeset -i to=$2

while ((from < to)); do
  ./create-job.sh "{\"id\":${from},\"eventTimestamp\":\"$(date +'%Y-%m-%dT%H:%M:%S.%NZ')\"}"
  ((from+=1))
done