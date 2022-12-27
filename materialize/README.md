# Reactive Spring Boot Kafka to R2DBC Example

Example Spring WebFlux project using reactive Kafka and R2DC to map events from topics to a postgres database table

Sample Spring Boot WebFlux project using reactive Spring Kafka and R2DC to map status events from two Kafka topics
to a postgres database table. The first topic contains "new" events, that lead to an `INSERT` in the database.
The second topic  contains status updates, that lead to an `UPDATE` in the database.

## Setup

### Kafka

- Broker: `kakfa-1:9092` via [docker-compose.yml](../docker/docker-compose.yml)
- Topics: `job-accepted,job-scheduled-A01,job-scheduled-A02,job-scheduled-A03,job-failed-A01,job-failed-A02,job-failed-A02,job-paused-B01,job-paused-B02,job-completed,job-notified,job-delivered`

### Postgres

- Server: `postgres:5432` via [docker-compose.yml](../docker/docker-compose.yml)
- Database: `states`
- User/password: `user/password`
- Table: `state_record`

## Testing

- For integration testing the kafka part `@EmbeddedKafka` is used.
- For integration testing the database part the following dependencies are used:
  - [org.testcontainers:postgresql](https://www.testcontainers.org/modules/databases/postgres/)
  - [org.testcontainers:r2dbc](https://www.testcontainers.org/modules/databases/r2dbc/)

### Command line testing

```bash
values=(accepted scheduled failed completed)
typeset -i i=0
while (( i < 10 )); do
  let id=$((1 + RANDOM))
  let status=$((RANDOM % 3))
  echo $id ${values[status]}
  ./generate-events.sh $id ${values[status]}
  (( i+=1 ))
done 

curl --silent 'http://localhost:8080/api/state-records?page=0&size=10&sort=ID,DESC' | jq . 
```

## Design decision

### UPSERT including overwrite prevention of newer jobs

The job events may not arrive in the "natural" order:
1. A job record state `UPDATE` (e.g. *JobScheduledEvent*) may arrive before the job record `INSERT` (*JobAcceptedEvent*).
   In this case, the *JobScheduledEvent* must perform a database `UPDATE` and the *JobAcceptedEvent* must be skipped.
2. A job record state `UPDATE` (e.g. *JobCompletedEvent*) may arrive before another job record state `UPDATE` (e.g. *JobScheduledEvent*),
   which is older. In this case, the older event must not overwrite the newer event.

Requirement 2. can be fulfilled by using a database UPDATE with a time check:
`UPDATE job_record SET j.status = :newStatus WHERE id = :id AND j.lastEventTimestamp < :newEventTimestamp`

For requirement 1. there are these approaches:

1. Using *SELECT* and then INSERT/UPDATE*
   - `SELECT * FROM job_record WHERE id = :id FOR UPDATE`
   - If found: `UPDATE job_record SET j.status = :newStatus WHERE id = :id AND j.lastEventTimestamp < :newEventTimestamp`
   - If not found: `INSERT INTO job_record VALUES (:id, :newStatus`
   - **Result:**: The `FOR UPDATE` doesn't help on the `INSERT`'s `unique constraint violation`, because a not existent row cannot be locked.
2. Using `INSERT`, duplicate check, then `UPDATE`
   - **Result:**: Does not work, because of rollback on the insert: `current transaction is aborted, commands ignored until end of transaction block`.
3. Using `INSERT ... ON CONFLICT DO NOTHING`, duplicate check, then `UPDATE`
   - **Result: Works**
4. Using `INSERT ... ON CONFLICT (id) DO UPDATE...`
   - **Result: Works**
5. Using `UPDATE`, count checked, then `INSERT`
   - Cannot be used together with requirement 2. We can not distinguish between "ID does not exist" and "there is a newer timestamp".
   - **Result:**: The `UPDATE` before `INSERT` doesn't help on the `INSERT`'s `unique constraint violation`.

The above approaches were checked with the default **Isolation Level**.

## TODO

- https://github.com/reactor/reactor-kafka/issues/227 and https://projectreactor.io/docs/kafka/release/reference/#kafka-source

## Config

- [KafkaConsumerConfig](src/main/java/com/giraone/jobs/materialize/config/KafkaConsumerConfig.java)
- [application.yml](src/main/resources/application.yml)
- [pom.xml](pom.xml)
