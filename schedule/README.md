# Job Scheduler

SCS based solution for job scheduler based on Staged Event Driven Architecture(SEDA).

## Setup

### Ports

- receiver: http://localhost:8090
- materialize: http://localhost:8091
- schedule: http://localhost:8092
- jobadmin: http://localhost:8093
- PGADMIN: http://localhost:8888

### Topics

```
job-accepted
job-accepted-err
job-scheduled-A01
job-scheduled-A02
job-scheduled-A03
job-scheduled-err
job-paused-B01
job-paused-B02
job-paused-err
job-completed
job-completed-err
job-delivered
job-failed-A01
job-failed-A02
job-failed-A03
job-failed-err
job-notified
job-notified-err
```

## TODOs

### Java 17 / Spring Boot 3.X

All projects are build for Java 17. The Spring Boot 3.0.0 migration is only partially done:

- [x] receiver
- [x] materialize
- [x] schedule - including Spring Cloud Stream 4.0.0
- [ ] jobadmin - Depends on JHipster

### Global

- [x] All events have *message keys* based on [TSID](https://github.com/f4b6a3/tsid-creator) to be time sorted and unique.
- [x] Event ID in messages are of type String (TSID). Only within the database it is a long value (64bit).
- [x] JobAdmin has to manage the buckets, no only the active/paused boolean
- [x] Processes use an n:m mapping to agents. For this there is an agentKey attribute in the process definition,
      which is delivered by the jobadmin service to the schedule service.
- [x] Display buckets in JobAdmin (materialize must handle this).
- [ ] Agent Key should be part of the events (done) and materialized database record.	  
- [ ] Add a requester id (string) to the (accepted) event and route it through all events and into the materialized view.
- [ ] Measure processing lag (already done in materialized view) and expose it as metric. Can be provided by *schedule* or *materialize* service.

### Receiver

- [x] Metrics (success and failure counter, request time) for all inbound REST requests to create new jobs.

### Materialize

- [x] Metrics (success and failure counter for insert and update, latency timer).
- [ ] What is better: **one** ReactiveKafkaConsumerTemplate with all topics or **two** separated for insert and update?
- [ ] Schedulers.parallel() vs Schedulers.boundedElastic() - see https://stackoverflow.com/questions/61304762/difference-between-boundedelastic-vs-parallel-scheduler
- [x] Prevent that older update events overwriting newer once - see StateRecordService.java.
- [x] Transactional safe solution for UPSERT (implemented with PostgreSQL's `INSERT ... ON CONFLICT IGNORE`).
- [ ] StateRecordService uses hard-coded '+ 1000L' for Process-ID (remove processId or map processKey to processId).

### Schedule

- [x] Metrics (success and failure counter for all processors, counter per topic for all sent messages).
- [x] The REST call to `jobadmin` for fetching the process states (paused, active) is tested with an integration test based on [WireMock](https://wiremock.org).
- [x] Pausing is added in state *accepted*. Here the job event are either passed to topic `scheduled` or `paused`.
- [x] The job states are fetched periodically using `@Schedule` in [PausedDecider.java](src/main/java/com/giraone/jobs/schedule/processor/PausedDecider.java)
- [x] When a state switches from *active* to *paused*, the processor Bxx switches from *running* to *paused*.
- [x] When a state switches from *paused* to *active*, the processor Bxx switches from *paused* to *running*.
- [ ] How to start the processor Bxx in state `running=true, paused=true` correctly? Property `auto-startup: false` leads to `running=false, paused=false`.
      Currently, this handle twice: with `auto-startup: false` and additionally via `ApplicationStartedEvent` in
     [EventProcessor.java](src/main/java/com/giraone/jobs/schedule/processor/EventProcessor.java)
- [ ] Fallback policy (`default=active` vs. `default=paused`) and fallback handling, when *jobadmin* is not reachable.
- [ ] Stopper implementation (full-stop after n errors or m errors within limit).
- [ ] Partition key - see https://spring.io/blog/2021/02/03/demystifying-spring-cloud-stream-producers-with-apache-kafka-partitions
- [ ] Is it possible to define processor using `destinationIsPattern=true` only once for all agents and paused buckets.
- [ ] Builder pattern for Job models.
- [ ] auto-create-topics: false not working with *StreamBridge*

### JobAdmin

- [x] The DTO uses the TSID String value, where the entity is a TSID long value.
- [x] During the database initialization 4 process definitions are automatically created: V001, V002, V003, V004.
      If this is not wanted, remove "faker" from `application-prod.yml`.
- [ ] Bug in sort by status.
- [ ] Switch off JPA caching.
- [ ] Inverse filtering, e.g. "all except NOTIFIED".

```

## Topologie

### Step "processSchedule"

- Input-Topic:  `job-accepted`
    - Payload: `JobAccepted`
- Error-Topics: `job-accepted-err`
- Output-Topic: `job-scheduled-A??` and `job-paused-B??`
    - Payload: `JobScheduled` and `JobPaused`

### Step "processResume"

- Input-Topic:  `job-paused-B??`
    - Payload: `JobPaused`
- Error-Topics: `job-paused-err`
- Output-Topic: `job-scheduled-A??`
    - Payload: `JobScheduled`
  
### Step "processAgentA??"

- Input-Topic:  `job-scheduled-A??`
    - Payload: `JobScheduled`
- Error-Topics: `job-scheduled-err`
- Output-Topic: `job-completed`
    - Payload: `JobCompleted`

### Step "processNotify"

- Input-Topic:  `job-completed`
  - Payload: `JobCompleted`
- Error-Topics: `job-completed-err`
- Output-Topic: `job-notified`
  - Payload: `JobNotified`
  
## Simple runtime tests

1. Create the topics: `../docker/kafka-create-topics.sh`
2. Write 2 input event for `job-accepted`: `../docker/generate-events.sh 1`
3. Read an output event from `job-scheduled-A01`: `../docker/kafka-create-topics.sh job-scheduled-A01`.
   This should display something like `{"requestId":"017ef6f360d5","startTime":"2022-02-14T07:37:32.245521","JobAcceptedEvent":{"name":"Hello"},"calculatedValue1":5}`

## Hints on the solution

### Disable Topic Auto Creation (Does not work yet!)

````yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          auto-create-topics: false
````

## Hints on Exception Handling

### Production Errors

One have to distinguish between
1. Exception when interacting with the broker, e.g. problems in producing event
2. Exceptions, like NPE, in the processor code

---

**Situation 1.** can be handled by defining a `default.production.exception.handler`. See [application.yml](src/main/resources/application.yml).

This can be done in the configuration on each function:

```yaml
functions:
  processSchedule:
    application-id: ${application.id.processSchedule}
    configuration:
      # Define producer exception handler
      default.production.exception.handler: <package>:CustomProductionExceptionHandler
```

or in the code

```java
public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    @Bean
    StreamsBuilderFactoryBeanConfigurer streamsCustomizer() {
        return new StreamsBuilderFactoryBeanConfigurer() {

            @Override
            public void configure(StreamsBuilderFactoryBean factoryBean) {

                Properties properties = factoryBean.getStreamsConfiguration();
                if (properties != null) {
                    properties.put(
                        StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG,
                        CustomProductionExceptionHandler.class);
                } else {
                    LOGGER.error("Cannot set CustomProductionExceptionHandler! Properties are null.");
                }
            }

            @Override
            public int getOrder() {
                return Integer.MAX_VALUE;
            }
        };
    }
}
```

---

**Situation 2.** is handled with `try/catch` around the processor steps and `StreamBridge.send("error-topic", badMessage)`

### Full Stop

Within the exception handling we stop the processing after too may errors, using `BindingsEndpoint`: `bindingsEndpoint.changeState(name, STOPPED);`. The processing can be restarted using the standard SCS admin endpoints.

The check, what "too many errors" means, can be defined using an interface:

```java
public interface ProcessingStopper {

    void reset();
    boolean addErrorAndCheckStop();
    boolean addSuccessAndCheckResume();
    Map<String,Object> getStatus();
}
```

### Global Uncaught Exception Handler

This works, but if the application reaches the *UncaughtExceptionHandler*, then the stream thread is already stopped
and it is too late to recover. See [sobychacko's answer on stackoverflow](https://stackoverflow.com/a/65743275/4404811).

```java
public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    @Bean
    StreamsBuilderFactoryBeanConfigurer streamsCustomizer() {

        return new StreamsBuilderFactoryBeanConfigurer() {

            @Override
            public void configure(StreamsBuilderFactoryBean factoryBean) {

                LOGGER.info("Initializing UncaughtExceptionHandler using EventProcessor.streamsCustomizer");
                factoryBean.setKafkaStreamsCustomizer(kafkaStreams -> kafkaStreams.setUncaughtExceptionHandler(
                    (thread, exception) -> {
                        LOGGER.error("UNCAUGHT EXCEPTION thread={}", thread.getName(), exception);
                    }));
            }

            @Override
            public int getOrder() {
                return Integer.MAX_VALUE;
            }
        };
    }
}
```

---
 
## Full-Stop (Start/Stop/Pause/Resume)

* Assuming application runs on port 8092.*

### Status

```bash
curl -H "Accept: application/json" -X GET http://localhost:8092/actuator/bindings/processSchedule-in-0
curl -H "Accept: application/json" -X GET http://localhost:8092/actuator/bindings/processResumeB01-in-0
```

### Stop

```bash
curl -d '{"state":"STOPPED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processSchedule-in-0
curl -d '{"state":"STOPPED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processResumeB01-in-0
```

### Start

```bash
curl -d '{"state":"STARTED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processSchedule-in-0
curl -d '{"state":"STARTED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processResumeB01-in-0
```

### Pause

```bash
curl -d '{"state":"PAUSED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processSchedule-in-0
curl -d '{"state":"PAUSED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processResumeB01-in-0
```

### Pause

```bash
curl -d '{"state":"RESUMED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processSchedule-in-0
curl -d '{"state":"RESUMED"}' -H "Content-Type: application/json" -X POST http://localhost:8092/actuator/bindings/processResumeB01-in-0
```

## Infos and Tutorials

### Spring Cloud Streams

- [Configuration Options - important for application.yml Settings](https://docs.spring.io/spring-cloud-stream/docs/3.2.6/reference/html/spring-cloud-stream.html#_configuration_options)
- [Stream Processing with Spring Cloud Stream and Apache Kafka Streams (6 Parts)](https://spring.io/blog/2019/12/09/stream-processing-with-spring-cloud-stream-and-apache-kafka-streams-part-6-state-stores-and-interactive-queries)
- [Testing](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_testing)
