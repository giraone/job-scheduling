# Job Scheduling Setup

A service mesh for a job scheduling example based on Kafka with a materialized view in Postgres.

## Contains the following services

- receive - REST endpoint to receive new jobs and put them to a Kafka topic `job-accepted`
- materialize - Kafka consumer to materialize the new jobs and job states into a Postgres database
- schedule - Spring Cloud Stream application to trigger jobs steps (schedule, pause, ...)
- jobadmin - [JHipster](https://www.jhipster.tech/) application to display the jobs from the materialized database

## Setup

### Services

- receive: http://localhost:8090
- materialize: http://localhost:8091
- schedule: http://localhost:8092
- jobadmin: http://localhost:8093

### Kafka

- Broker: `kakfa-1:9092` via [docker-compose.yml](docker/docker-compose.yml)
- Topics: see [docker/setup-kafka-topics.sh](docker/setup-kafka-topics.sh)

### Postgres

- Server: `postgres:5432` via [docker-compose.yml](docker/docker-compose.yml)
- Database: `states`
- User/password: `user/password`
- Tables: `job_record`, `process` - 