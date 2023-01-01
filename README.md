# Job Scheduling Setup

A service mesh for a job scheduling example based on Kafka with a materialized view in Postgres.

## Contains the following services

- [receive](receive) - REST endpoint to receive new jobs and put them to a Kafka topic `job-accepted`
- [materialize](materialize) - Kafka consumer to materialize the new jobs events and job state update events into a Postgres database
- [schedule](schedule) - Spring Cloud Stream application to trigger jobs steps
  - schedule new jobs
  - pause new jobs, when process is "on hold"
  - process
  - notify, when process completes
- [jobadmin](jobadmin) - [JHipster](https://www.jhipster.tech/) application
  - to display the jobs from the materialized database
  - set processes "on hold"

## Architecural and design decisions

- The content of the Kafka topics is the *"single source of truth"* for the job state. 
- The materialized view is only an *eventual consistent* follower.
- The receiver assigns a new job id to every received job.
  This ID is based on [TSID](https://github.com/f4b6a3/tsid-creator).

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
