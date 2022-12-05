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

## TODO

- https://github.com/reactor/reactor-kafka/issues/227 and https://projectreactor.io/docs/kafka/release/reference/#kafka-source

## Config

- [KafkaConsumerConfig](src/main/java/com/giraone/jobs/materialize/config/KafkaConsumerConfig.java)
- [application.yml](src/main/resources/application.yml)
- [pom.xml](pom.xml)
