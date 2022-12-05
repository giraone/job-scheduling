# Reactive HTTP Endpoint to Kafka Topic

Example Spring WebFlux project providing an endpoint to send events to Kafka.

## Setup

### Kafka

- Broker: `kakfa-1:9092` via [docker-compose.yml](../docker/docker-compose.yml)
- Topics: `job-accepted`

### JMeter testing

*Example on Windows 10 with i7-4910 2.9GHz, Kafka in Docker Desktop*

| Threads | Average (ms) |  RPS |
|--------:|-------------:|-----:|
|       1 |            2 |  370 |
|       2 |            3 |  490 |
|       4 |            4 | 1100 |
|       8 |            5 | 1350 |
|      16 |            7 | 1800 |
|      32 |           12 | 2300 |

### Command line testing

```bash
cd src/test/curl
./create-jobs.sh 100 200
```

## TODO

- https://github.com/reactor/reactor-kafka/issues/227 and https://projectreactor.io/docs/kafka/release/reference/#kafka-source

## Config

- [application.yml](src/main/resources/application.yml)
- [pom.xml](pom.xml)
