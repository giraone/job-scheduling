package com.giraone.jobs.materialize.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobStatusChangedEvent;
import com.giraone.jobs.materialize.common.ObjectMapperBuilder;
import com.giraone.jobs.materialize.model.DatabaseOperation;
import com.giraone.jobs.materialize.model.DatabaseResult;
import com.giraone.jobs.materialize.model.JobRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConsumerService implements CommandLineRunner {

    private static final boolean UPSERT = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerService.class);
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

    private final StateRecordService stateRecordService;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateInserts;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateUpdates;

    private final Scheduler schedulerInserts = Schedulers.newBoundedElastic(10, Integer.MAX_VALUE, "schedulerInserts");
    private final Scheduler schedulerUpdates = Schedulers.newBoundedElastic(1, Integer.MAX_VALUE, "schedulerUpdates");

    private final Map<String, Long> offSetsPerPartition = new HashMap<>();

    public ConsumerService(
        StateRecordService stateRecordService,
        @Qualifier("INSERTS") ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateInserts,
        @Qualifier("UPDATES") ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplateUpdates
    ) {
        this.reactiveKafkaConsumerTemplateInserts = reactiveKafkaConsumerTemplateInserts;
        this.reactiveKafkaConsumerTemplateUpdates = reactiveKafkaConsumerTemplateUpdates;
        this.stateRecordService = stateRecordService;
    }

    private Flux<DatabaseResult> consumeFromInsertTopicAndWriteToDatabase() {

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

    private Flux<DatabaseResult> consumeFromUpdateTopicsAndWriteToDatabase() {

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

    private Mono<DatabaseResult> consumeNewEvent(ReceiverRecord<String, String> consumerRecord) {

        final String messageKey = consumerRecord.key();
        LOGGER.info("NEW TOPIC={}, KEY={}, MESSAGE={}", consumerRecord.topic(), messageKey, consumerRecord.value());
        return parseNewEvent(consumerRecord.value())
            .flatMap(event -> storeStateForNewJob(event, Instant.now(), event.getProcessKey()))
            .doOnSuccess(databaseResult -> {
                logDatabaseResult(databaseResult, consumerRecord);
                consumerRecord.receiverOffset().commit().then(Mono.just(databaseResult));
            })
            .onErrorResume(throwable -> {
                final DatabaseResult result = new DatabaseResult(messageKey, false, DatabaseOperation.insert);
                logError(result, consumerRecord, throwable);
                consumerRecord.receiverOffset().commit();
                LOGGER.error("Erroneous message committed with partition={}, offset={}",
                    consumerRecord.receiverOffset().topicPartition(), consumerRecord.receiverOffset().offset());
                return MonoOperator.just(result);
            });
    }

    private Mono<DatabaseResult> consumeUpdateEvent(ReceiverRecord<String, String> consumerRecord) {

        final String messageKey = consumerRecord.key();
        LOGGER.info("UPD TOPIC={}, KEY={}, MESSAGE={}", consumerRecord.topic(), messageKey, consumerRecord.value());
        return parseChangeEvent(consumerRecord.value())
            .flatMap(this::storeStateForExistingJob)
            .doOnSuccess(databaseResult -> {
                logDatabaseResult(databaseResult, consumerRecord);
                consumerRecord.receiverOffset().commit().then(Mono.just(databaseResult));
            })
            .onErrorResume(throwable -> {
                final DatabaseResult result = new DatabaseResult(messageKey, false, DatabaseOperation.update);
                logError(result, consumerRecord, throwable);
                consumerRecord.receiverOffset().commit();
                return MonoOperator.just(result);
            });
    }

    private void logDatabaseResult(DatabaseResult databaseResult, ReceiverRecord<String, String> consumerRecord) {
        final String topicPartition = consumerRecord.receiverOffset().topicPartition().topic()
            + "/" + consumerRecord.receiverOffset().topicPartition().partition();
        final Long offset = consumerRecord.receiverOffset().offset();
        final Long lastOffset = offSetsPerPartition.get(topicPartition);
        // TODO
        //if (lastOffset == null || offset - lastOffset == 99) {
            LOGGER.info("Commit % 100 with topic/partition={}, offset={}", topicPartition, offset);
        //}
        offSetsPerPartition.put(topicPartition, offset);
        if (databaseResult.isSuccess()) {
            LOGGER.debug("Successfully consumed and committed {} {}", databaseResult.getOperation(), databaseResult.getEntityId());
        } else {
            LOGGER.warn("Database failure for {} id={} with topic/partition={}, offset={}",
                databaseResult.getOperation(), databaseResult.getEntityId(), topicPartition, offset);
        }
    }

    private void logError(DatabaseResult databaseResult, ReceiverRecord<String, String> consumerRecord, Throwable throwable) {
        final String topicPartition = consumerRecord.receiverOffset().topicPartition().topic()
            + "/" + consumerRecord.receiverOffset().topicPartition().partition();
        final Long offset = consumerRecord.receiverOffset().offset();
        offSetsPerPartition.put(topicPartition, offset);
        LOGGER.warn("Error for {} with topic/partition={}, offset={}, error={}",
            databaseResult.getOperation(), topicPartition, offset, throwable.getMessage());
    }

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

    private Mono<DatabaseResult> storeStateForNewJob(JobAcceptedEvent jobAcceptedEvent, Instant now, String processKey) {

        if (LOGGER.isDebugEnabled()) {
            long latency = jobAcceptedEvent.getEventTimestamp() != null
                ? now.toEpochMilli() - jobAcceptedEvent.getEventTimestamp().toEpochMilli()
                : -1;
            LOGGER.debug("INSERT id={}, eventTimestamp={} to state={}, latency={}ms",
                jobAcceptedEvent.getId(), jobAcceptedEvent.getEventTimestamp(), JobRecord.STATE_accepted, latency);
        }
        return stateRecordService.insert(jobAcceptedEvent.getId(), jobAcceptedEvent.getEventTimestamp(), processKey)
            .map(stateRecord -> new DatabaseResult(jobAcceptedEvent.getId(), true, DatabaseOperation.insert));
    }

    private Mono<DatabaseResult> storeStateForExistingJob(JobStatusChangedEvent jobChangedEvent) {

        if (LOGGER.isDebugEnabled()) {
            long latency = jobChangedEvent.getEventTimestamp() != null
                ? Instant.now().toEpochMilli() - jobChangedEvent.getEventTimestamp().toEpochMilli()
                : -1;
            LOGGER.debug("UPDATE id={}, eventTimestamp={} to state={}, latency={}ms",
                jobChangedEvent.getId(), jobChangedEvent.getEventTimestamp(), jobChangedEvent.getStatus(), latency);
        }
        if (UPSERT) {
            return stateRecordService.upsert(jobChangedEvent.getId(), jobChangedEvent.getJobAcceptedTimestamp(), jobChangedEvent.getProcessKey(),
                    jobChangedEvent.getStatus(), jobChangedEvent.getEventTimestamp(), jobChangedEvent.getPausedBucketKey())
                .map(updateCount -> new DatabaseResult(jobChangedEvent.getId(), updateCount != null && updateCount > 0, DatabaseOperation.update));
        } else {
            return stateRecordService.update(jobChangedEvent.getId(), jobChangedEvent.getStatus(),
                    jobChangedEvent.getEventTimestamp(), jobChangedEvent.getPausedBucketKey())
                .map(updateCount -> new DatabaseResult(jobChangedEvent.getId(), updateCount != null && updateCount > 0, DatabaseOperation.update));
        }
    }

    @Override
    public void run(String... args) {

        LOGGER.info("STARTING ConsumerService for INSERTS on {}", schedulerInserts);
        // we have to trigger the consumption
        consumeFromInsertTopicAndWriteToDatabase()
            // publish on scheduler, because storeState uses block() and so receiver thread itself is not blocked
            .publishOn(schedulerInserts)
            .subscribe();

        LOGGER.info("STARTING ConsumerService for UPDATES on {}", schedulerUpdates);
        consumeFromUpdateTopicsAndWriteToDatabase()
            // publish on scheduler, because storeState uses block() and so receiver thread itself is not blocked
            .publishOn(schedulerUpdates)
            .subscribe();
    }
}