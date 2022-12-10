# Job Scheduler

SCS based solution for job scheduler based on Staged Event Driven Architecture(SEDA).

## TODO

- [x] Pausing is added in state *accepted*. Here the job event are either passed to topic `scheduled` or `paused`.
- [ ] Pause/Resume added
- [ ] Partition key - see https://spring.io/blog/2021/02/03/demystifying-spring-cloud-stream-producers-with-apache-kafka-partitions
- [ ] Message key
- [ ] Testing the REST call to `jobadmin` is currently only mocked. An integration test with *MockServer* should be added.

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
    - 
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

### Disable Topic Auto Creation

````yaml
spring:
  cloud:
    stream:
      kafka:
        streams:
          binder:
            auto-create-topics: false # We do not want topic auto creation
````

If the topics are not created, processing and application stops with the following output:

```
o.s.c.s.b.k.p.KafkaTopicProvisioner      : Auto creation of topics is disabled.
...
o.a.k.s.p.i.StreamsPartitionAssignor     : Source topic xxx is missing/unknown during rebalance, please make sure all source topics have been pre-created before starting the Streams application. Returning error INCOMPLETE_SOURCE_TOPIC_METADATA
```

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
                  default.production.exception.handler: exceptions.de.datev.ediio.vera.CustomProductionExceptionHandler
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

This works, but if the application reaches the *UncaughtExcpetionHandler*, then the stream thread is already stopped
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
                        LOGGER.error(">>> UNCAUGHT EXCEPTION thread={}", thread.getName(), exception);
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

* Assuming application runs on port 8070.*

### Status

```bash
curl -H "Accept: application/json" -X GET http://localhost:8070/actuator/bindings/processSchedule-in-0
```

### Stop

```bash
curl -d '{"state":"STOPPED"}' -H "Content-Type: application/json" -X POST http://localhost:8070/actuator/bindings/processSchedule-in-0
```

### Start

```bash
curl -d '{"state":"STARTED"}' -H "Content-Type: application/json" -X POST http://localhost:8070/actuator/bindings/processSchedule-in-0
```

### Pause - only if `pausable=true`

```bash
curl -d '{"state":"PAUSED"}' -H "Content-Type: application/json" -X POST http://localhost:8070/actuator/bindings/processSchedule-in-0
```

### Pause - only if `pausable=true`

```bash
curl -d '{"state":"RESUMED"}' -H "Content-Type: application/json" -X POST http://localhost:8070/actuator/bindings/processSchedule-in-0
```

## Infos and Tutorials

### Spring Cloud Streams

- [Configuration Options - important for application.yml Settings](https://docs.spring.io/spring-cloud-stream/docs/3.2.6/reference/html/spring-cloud-stream.html#_configuration_options)
- [Stream Processing with Spring Cloud Stream and Apache Kafka Streams (6 Parts)](https://spring.io/blog/2019/12/09/stream-processing-with-spring-cloud-stream-and-apache-kafka-streams-part-6-state-stores-and-interactive-queries)
- [Testing](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_testing)
