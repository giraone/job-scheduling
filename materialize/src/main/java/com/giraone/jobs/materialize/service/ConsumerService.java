package com.giraone.jobs.materialize.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobStatusChangedEvent;
import com.giraone.jobs.materialize.common.ObjectMapperBuilder;
import com.giraone.jobs.materialize.persistence.DatabaseOperation;
import com.giraone.jobs.materialize.persistence.DatabaseResult;
import com.giraone.jobs.materialize.persistence.JobRecord;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.ReceiverRecord;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConsumerService implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerService.class);
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

    private static final String METRICS_PREFIX = "materialize";
    private static final String METRICS_JOBS_INSERT_SUCCESS = "jobs.insert.success";
    private static final String METRICS_JOBS_INSERT_FAILURE = "jobs.insert.failure";
    private static final String METRICS_JOBS_UPDATE_SUCCESS = "jobs.update.success";
    private static final String METRICS_JOBS_UPDATE_FAILURE = "jobs.update.failure";

    private final StateRecordService stateRecordService;
    private final MeterRegistry meterRegistry;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateInserts;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateUpdates;

    private final Scheduler scheduler = Schedulers.newBoundedElastic(
        Runtime.getRuntime().availableProcessors() - 1, Integer.MAX_VALUE, "schedulers");

    private final Map<String, Long> offSetsPerPartition = new HashMap<>();

    private Counter insertSuccessCounter;
    private Counter insertFailureCounter;
    private Counter updateSuccessCounter;
    private Counter updateFailureCounter;


    public ConsumerService(
        StateRecordService stateRecordService,
        MeterRegistry meterRegistry,
        @Qualifier("INSERTS") ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateInserts,
        @Qualifier("UPDATES") ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateUpdates
    ) {
        this.stateRecordService = stateRecordService;
        this.meterRegistry = meterRegistry;
        this.reactiveKafkaConsumerTemplateInserts = reactiveKafkaConsumerTemplateInserts;
        this.reactiveKafkaConsumerTemplateUpdates = reactiveKafkaConsumerTemplateUpdates;
    }

    //------------------------------------------------------------------------------------------------------------------

    @PostConstruct
    private void init() {
        this.insertSuccessCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_INSERT_SUCCESS)
            .description("Counter for all new jobs, that are successfully materialized to Postgres.")
            .register(meterRegistry);
        this.insertFailureCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_INSERT_FAILURE)
            .description("Counter for all new jobs, that were not materialized to Postgres.")
            .register(meterRegistry);
        this.updateSuccessCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_UPDATE_SUCCESS)
            .description("Counter for all updated jobs, that are successfully materialized to Postgres.")
            .register(meterRegistry);
        this.updateFailureCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_UPDATE_FAILURE)
            .description("Counter for all updated jobs, that were not materialized to Postgres.")
            .register(meterRegistry);
    }

    @Override
    public void run(String... args) {

        LOGGER.info("STARTING ConsumerService for INSERTS on {}", scheduler);
        // we have to trigger the consumption
        consumeFromInsertTopicAndWriteToDatabase()
            // publish on scheduler, because storeState uses block() and so receiver thread itself is not blocked
            .publishOn(scheduler)
            .subscribe();

        LOGGER.info("STARTING ConsumerService for UPDATES on {}", scheduler);
        consumeFromUpdateTopicsAndWriteToDatabase()
            // publish on scheduler, because storeState uses block() and so receiver thread itself is not blocked
            .publishOn(scheduler)
            .subscribe();
    }

    public Flux<DatabaseResult> consumeFromInsertTopicAndWriteToDatabase() {

        return reactiveKafkaConsumerTemplateInserts
            .receive()
            .doOnNext(consumerRecord -> LOGGER.debug("received I key={}, value={}, timestamp={} from topic={}, partition={}, offset={}",
                consumerRecord.key(),
                consumerRecord.value(),
                consumerRecord.timestamp(),
                consumerRecord.topic(),
                consumerRecord.partition(),
                consumerRecord.offset())
            )
            .concatMap(this::consumeNewEvent);
    }

    public Flux<DatabaseResult> consumeFromUpdateTopicsAndWriteToDatabase() {

        return reactiveKafkaConsumerTemplateUpdates
            .receive()
            .doOnNext(consumerRecord -> LOGGER.debug("received U key={}, value={}, timestamp={} from topic={}, partition={}, offset={}",
                consumerRecord.key(),
                consumerRecord.value(),
                consumerRecord.timestamp(),
                consumerRecord.topic(),
                consumerRecord.partition(),
                consumerRecord.offset())
            )
            .concatMap(this::consumeUpdateEvent);
    }

    //------------------------------------------------------------------------------------------------------------------

    private Mono<DatabaseResult> consumeNewEvent(ReceiverRecord<String, String> consumerRecord) {

        final String messageKey = consumerRecord.key();
        LOGGER.debug(">>> NEW KEY={}, TOPIC={}, MESSAGE={}", messageKey, consumerRecord.topic(), consumerRecord.value());
        return parseNewEvent(consumerRecord.value())
            .flatMap(event -> storeStateForNewJob(event, event.getProcessKey()))
            .doOnSuccess(databaseResult -> {
                this.insertSuccessCounter.increment();
                logDatabaseResult(databaseResult, consumerRecord);
                consumerRecord.receiverOffset().commit().then(Mono.just(databaseResult));
            })
            .onErrorResume(throwable -> {
                this.insertFailureCounter.increment();
                final DatabaseResult databaseResult = new DatabaseResult(messageKey, false, DatabaseOperation.insert);
                logError(databaseResult, consumerRecord, throwable);
                return consumerRecord.receiverOffset().commit().then(Mono.just(databaseResult));
            });
    }

    private Mono<DatabaseResult> consumeUpdateEvent(ReceiverRecord<String, String> consumerRecord) {

        final String messageKey = consumerRecord.key();
        LOGGER.debug(">>> UPD KEY={}, TOPIC={}, MESSAGE={}", messageKey, consumerRecord.topic(), consumerRecord.value());
        return parseChangeEvent(consumerRecord.value())
            .flatMap(this::storeStateForExistingJob)
            .doOnSuccess(databaseResult -> {
                this.updateSuccessCounter.increment();
                logDatabaseResult(databaseResult, consumerRecord);
                consumerRecord.receiverOffset().commit().then(Mono.just(databaseResult));
            })
            .onErrorResume(throwable -> {
                this.updateFailureCounter.increment();
                final DatabaseResult databaseResult = new DatabaseResult(messageKey, false, DatabaseOperation.update);
                logError(databaseResult, consumerRecord, throwable);
                return consumerRecord.receiverOffset().commit().then(Mono.just(databaseResult));
            });
    }

    //------------------------------------------------------------------------------------------------------------------

    private Mono<DatabaseResult> logError(String messageKey, ReceiverRecord<String, String> consumerRecord, Throwable throwable) {
        final DatabaseResult databaseResult = new DatabaseResult(messageKey, false, DatabaseOperation.update);
        logError(databaseResult, consumerRecord, throwable);
        consumerRecord.receiverOffset().commit();
        return Mono.just(databaseResult);
    }

    private void logError(DatabaseResult databaseResult, ReceiverRecord<String, String> consumerRecord, Throwable throwable) {
        final String topicPartition = consumerRecord.receiverOffset().topicPartition().topic()
            + "/" + consumerRecord.receiverOffset().topicPartition().partition();
        final Long offset = consumerRecord.receiverOffset().offset();
        offSetsPerPartition.put(topicPartition, offset);
        if (throwable == null || throwable instanceof R2dbcDataIntegrityViolationException) {
            LOGGER.debug("Error for {} with topic/partition={}, offset={}, error={}",
                databaseResult.getOperation(), topicPartition, offset, throwable != null ? throwable.getMessage() : "?");
        } else {
            LOGGER.warn("Error for {} with topic/partition={}, offset={}, error={}",
                databaseResult.getOperation(), topicPartition, offset, throwable.getMessage());
        }
    }

    private void logDatabaseResult(DatabaseResult databaseResult, ReceiverRecord<String, String> consumerRecord) {
        final String topicPartition = consumerRecord.receiverOffset().topicPartition().topic()
            + "/" + consumerRecord.receiverOffset().topicPartition().partition();
        final Long offset = consumerRecord.receiverOffset().offset();
        final Long lastOffset = offSetsPerPartition.get(topicPartition);
        if (lastOffset == null || offset - lastOffset == 99) {
            LOGGER.info("Committed events at topic/partition={}, offset={}, lastOffset={}", topicPartition, offset, lastOffset);
        }
        offSetsPerPartition.put(topicPartition, offset);
        LOGGER.debug("Successfully consumed and committed {} {}",
            databaseResult.getOperation(), databaseResult.getEntityId());
    }

    //------------------------------------------------------------------------------------------------------------------

    private Mono<JobAcceptedEvent> parseNewEvent(String message) {
        try {
            return Mono.just(objectMapper.readValue(message, JobAcceptedEvent.class));
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException(e));
        }
    }

    private Mono<JobStatusChangedEvent> parseChangeEvent(String message) {
        try {
            final JobStatusChangedEvent jobStatusChangedEvent = objectMapper.readValue(message, JobStatusChangedEvent.class);
            return Mono.just(jobStatusChangedEvent);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException(e));
        }
    }

    private Mono<DatabaseResult> storeStateForNewJob(JobAcceptedEvent jobAcceptedEvent, String processKey) {

        LOGGER.debug("Mono for INSERT id={}, eventTimestamp={}, state={}",
            jobAcceptedEvent.getId(), jobAcceptedEvent.getEventTimestamp(), JobRecord.STATE_accepted);
        return stateRecordService.insertAcceptedIgnoreConflict(jobAcceptedEvent.getId(), jobAcceptedEvent.getJobAcceptedTimestamp(), processKey)
            .doOnSuccess(count -> {
                if (count == 1) {
                    LOGGER.debug("INSERTED id={}, lastEventTimestamp={}, state={}",
                        jobAcceptedEvent.getId(), jobAcceptedEvent.getEventTimestamp(), jobAcceptedEvent.getStatus());
                }
            })
            .map(count -> new DatabaseResult(jobAcceptedEvent.getId(), true, DatabaseOperation.insert));
    }

    private Mono<DatabaseResult> storeStateForExistingJob(JobStatusChangedEvent jobChangedEvent) {

        LOGGER.debug("Mono for UPDATE id={}, eventTimestamp={}, state={}",
            jobChangedEvent.getId(), jobChangedEvent.getEventTimestamp(), jobChangedEvent.getStatus());
        return stateRecordService.insertIgnoreConflictThenUpdate(jobChangedEvent.getId(), jobChangedEvent.getJobAcceptedTimestamp(), jobChangedEvent.getProcessKey(),
                jobChangedEvent.getStatus(), jobChangedEvent.getEventTimestamp(), jobChangedEvent.getPausedBucketKey())
            .doOnSuccess(updateCount -> {
                if (updateCount != null && updateCount > 0) {
                    LOGGER.debug("UPDATED id={}, lastEventTimestamp={}, state={}",
                        jobChangedEvent.getId(), jobChangedEvent.getEventTimestamp(), jobChangedEvent.getStatus());
                }
            })
            .map(updateCount -> new DatabaseResult(jobChangedEvent.getId(), updateCount != null && updateCount > 0, DatabaseOperation.update));
    }
}