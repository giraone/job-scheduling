#!/bin/bash

echo "The application will start in ${STARTUP_SLEEP}s..." && sleep ${STARTUP_SLEEP}

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
    local var="$1"
    local fileVar="${var}_FILE"
    local def="${2:-}"
    if [[ ${!var:-} && ${!fileVar:-} ]]; then
        echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
        exit 1
    fi
    local val="$def"
    if [[ ${!var:-} ]]; then
        val="${!var}"
    elif [[ ${!fileVar:-} ]]; then
        val="$(< "${!fileVar}")"
    fi

    if [[ -n $val ]]; then
        export "$var"="$val"
    fi

    unset "$fileVar"
}

file_env 'SPRING_KAFKA_BOOTSTRAP_SERVERS'

exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/urandom -cp /app/resources/:/app/classes/:/app/libs/* "com.giraone.jobs.receiver.ReceiverApplication" "$@"
