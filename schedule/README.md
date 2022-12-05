# Job Scheduler

SCS based solution for job scheduler based on Staged Event Driven Architecture(SEDA).

## TODO

- [x] Pausiert wird nur im *accepted* Status. Dann geht es nicht nach `scheduled`, sondern nach `paused`.
- [x] Resume geht mit Alternative "periodisch".
- [ ] Key der Messages? Immer Auftrags-ID?
- [ ] Stream-Partitioner aktuell Hash auf Key. Die verfahren sollten auf verschiedene Partitionen aufgeteilt sein,
  aber idealerweise alle Events eines Key immer im gleichen Partitions-Index. Das würde Trouble-Shooting erleichtern.
- [ ] Der Rest-Call zu `jobadmin` wird im Test nur ge-mocked. Besser wäre ein Test mit MockServer.

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

1. Create the topics: `../docker/setup_kafka_topic.sh`
2. Write an input event into `job-accepted`: `jq -rc . src/test/resources/testdata/accepted-in.json | $KAFKA/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic accepted`
3. Read an output event from `job-scheduled-A01`: `$KAFKA/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic scheduled --from-beginning`.
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

### Partition support on the outbound (see 2.12)

The partitioner must be created on the output:

```yaml
spring:
  cloud:
    stream:
      kafka:
        streams:
          binder:
           processSchedule-out-0:
             producer: # producer properties on each function (output) level
               streamPartitionerBeanName: streamPartitionerDefault
           processAgent-in-0:
             producer: # producer properties on each function (output) level
               streamPartitionerBeanName: streamPartitionerDefault
```

## Hints on Exception Handling

### Deserialization Errors 

See
- [Apache Kafka Streams documentation on handling deserialization errors](https://docs.confluent.io/platform/current/streams/faq.html#streams-faq-failure-handling-deserialization-errors)
- [Spring Cloud Stream documentation on handling deserialization errors](https://docs.spring.io/spring-cloud-stream-binder-kafka/docs/3.1.4/reference/html/spring-cloud-stream-binder-kafka.html#_handling_deserialization_exceptions_in_the_binder)
- [Blog on Stream Processing with Spring Cloud Stream and Apache Kafka Streams. Part 4 - Error Handling](https://spring.io/blog/2019/12/05/stream-processing-with-spring-cloud-stream-and-apache-kafka-streams-part-4-error-handling)

Setup the DLQ. This can be done

- globally
- for each consumer

```yaml
spring:
  cloud:
    stream:
      kafka:
        streams:
          binder:
            # Setup DLQ globally
            deserialization-exception-handler: sendtodlq
          bindings:
            processSchedule-in-0:
              consumer:
                # Setup DLQ for each consumer
                deserialization-exception-handler: sendtodlq
                dlqName: ${application.topic.topicAccepted}.dlq
            processAgent-in-0:    
              consumer:
                # Setup DLQ for each consumer
                deserialization-exception-handler: sendtodlq
                dlqName: ${application.topic.topicScheduled}.dlq
```

### Production Errors

See
- [Apache Kafka Streams documentation on default production handler](https://docs.confluent.io/platform/current/streams/developer-guide/config-streams.html#default-production-exception-handler)
- [Spring Cloud Stream documentation on handling production errors](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream-binder-kafka/3.0.0.RELEASE/reference/html/spring-cloud-stream-binder-kafka.html#:~:text=as%20outlined%20above.-,2.13.2.%20Using%20customizer%20to%20register%20a%20production%20exception%20handler,-In%20the%20error)

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

The documentation is in [https://cloud.spring.io/spring-cloud-static/spring-cloud-stream-binder-kafka](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream-binder-kafka/3.0.4.RELEASE/reference/html/spring-cloud-stream-binder-kafka.html#_streamsbuilderfactorybean_customizer).

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

## Infos und Tutorials

### Kafka Streams

- [Learn Stream Processing With Kafka Streams - Stateless operations](https://betterprogramming.pub/learn-stream-processing-with-kafka-streams-stateless-operations-2111080e6c53)
- [How to Use Stateful Operations in Kafka Streams](https://betterprogramming.pub/how-to-use-stateful-operations-in-kafka-streams-1cff4da41329)

### Spring Cloud Streams

- [Configuration Options - wichtig für application.yml Settings](https://docs.spring.io/spring-cloud-stream/docs/3.1.4/reference/html/spring-cloud-stream.html#_configuration_options)
- [Stream Processing with Spring Cloud Stream and Apache Kafka Streams (6 Parts)](https://spring.io/blog/2019/12/09/stream-processing-with-spring-cloud-stream-and-apache-kafka-streams-part-6-state-stores-and-interactive-queries)
- [Testing](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_testing)
