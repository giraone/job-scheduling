package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.persistence.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.data.relational.core.query.Query.query;

@SpringBootTest
@DirtiesContext
class StateRecordServiceIntTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateRecordServiceIntTest.class);
    private static final TemporalUnitOffset toleratedInstantOffset = within(1, ChronoUnit.MILLIS);

    @Autowired
    private StateRecordService stateRecordService;

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Test
    void updateDoesNotOverwriteNewerJobs() {

        r2dbcEntityTemplate.delete(JobRecord.class).matching(query(Criteria.empty())).all().block();
        assertThat(r2dbcEntityTemplate.select(JobRecord.class).count().block()).isEqualTo(0L);

        // arrange
        Tsid id = TsidCreator.getTsid256();
        Instant now = Instant.now();
        Instant createdTimeStamp = now.minusMillis(10);
        Instant completedTimeStamp = now.minusMillis(5);
        Instant notifyTimeStamp = now.minusMillis(1);

        // arrange - insert
        JobRecord jobRecord = stateRecordService.insertAccepted(id.toString(), createdTimeStamp, "V001").block();
        assertThat(jobRecord).isNotNull();

        // act - a newer event
        Integer updateCount1 = stateRecordService.update(true, id.toLong(), "NOTIFIED", notifyTimeStamp, null).block();
        assertThat(updateCount1).isEqualTo(1);

        // act - an older event
        Integer updateCount2 = stateRecordService.update(true, id.toLong(), "COMPLETED", completedTimeStamp, null).block();
        assertThat(updateCount2).isEqualTo(0);

        // assert
        Long countAll = r2dbcEntityTemplate.select(JobRecord.class).count().block();
        assertThat(countAll).isEqualTo(1);
    }

    //------------------------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "insertIgnoreConflictThenUpdate",
    })
    void concurrentExecutionOfInsert(String method) throws ExecutionException, InterruptedException {

        // arrange
        r2dbcEntityTemplate.delete(JobRecord.class).matching(query(Criteria.empty())).all().block();
        assertThat(r2dbcEntityTemplate.select(JobRecord.class).count().block()).isEqualTo(0L);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Tsid id = TsidCreator.getTsid256();
        String idString = id.toString();
        Instant now = Instant.now();
        Instant jobAcceptedTimestamp = now.minusSeconds(10);
        String processKey = "V001";
        String pausedBucketKey = null;
        String state1 = "SCHEDULED";
        Instant lastEventTimestamp1 = now.minusSeconds(2);
        String state2 = "COMPLETED";
        Instant lastEventTimestamp2 = now.minusSeconds(1);

        // act
        Mono<Integer> mono1 = getMethod(method, idString, jobAcceptedTimestamp, processKey, pausedBucketKey, state1, lastEventTimestamp1);
        Mono<Integer> mono2 = getMethod(method, idString, jobAcceptedTimestamp, processKey, pausedBucketKey, state2, lastEventTimestamp2);
        Future<Integer> future1 = executorService.submit(() -> mono1.block());
        Future<Integer> future2 = executorService.submit(() -> mono2.block());

        // assert
        assertThat(future1).succeedsWithin(Duration.ofSeconds(1));
        assertThat(future2).succeedsWithin(Duration.ofSeconds(1));
        LOGGER.debug("Call for {}: {}", state1, future1.get());
        LOGGER.debug("Call for {}: {}", state2, future2.get());
        assertThat(future1.get()).isEqualTo(0);
        assertThat(future2.get()).isEqualTo(1);

        JobRecord record = r2dbcEntityTemplate.select(query(Criteria.empty()), JobRecord.class).blockFirst();
        assertThat(record).isNotNull();
        assertThat(record.getId()).isEqualTo(id.toLong());
        assertThat(record.getStatus()).isEqualTo("COMPLETED");
        assertThat(record.getJobAcceptedTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(record.getLastEventTimestamp()).isCloseTo(lastEventTimestamp2, toleratedInstantOffset);
        assertThat(record.getLastRecordUpdateTimestamp()).isCloseTo(now, within(1000, ChronoUnit.MILLIS));
    }

    @ParameterizedTest
    @CsvSource({
        "findAndUpdateOrInsert",
        "insertUpdate",
        "insertIgnoreConflictThenUpdate",
        "insertOnConflictUpdate",
        "upsert"
    })
    void singleExecution(String method) throws Exception {

        // arrange
        r2dbcEntityTemplate.delete(JobRecord.class).matching(query(Criteria.empty())).all().block();
        assertThat(r2dbcEntityTemplate.select(JobRecord.class).count().block()).isEqualTo(0L);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Tsid id = TsidCreator.getTsid256();
        String idString = id.toString();
        Instant now = Instant.now();
        Instant jobAcceptedTimestamp = now.minusSeconds(10);
        String processKey = "V001";
        String pausedBucketKey = null;
        String state1 = "SCHEDULED";
        Instant lastEventTimestamp1 = now.minusSeconds(2);

        // act
        Mono<Integer> mono1 = getMethod(method, idString, jobAcceptedTimestamp, processKey, pausedBucketKey, state1, lastEventTimestamp1);
        Future<Integer> future1 = executorService.submit(() -> mono1.block());

        // assert
        assertThat(future1).succeedsWithin(Duration.ofSeconds(1));
        LOGGER.debug("Call for {}: {}", state1, future1.get());
        assertThat(future1.get()).isEqualTo(1);

        JobRecord record = r2dbcEntityTemplate.select(query(Criteria.empty()), JobRecord.class).blockFirst();
        assertThat(record).isNotNull();
        assertThat(record.getId()).isEqualTo(id.toLong());
        assertThat(record.getStatus()).isEqualTo("SCHEDULED");
        assertThat(record.getJobAcceptedTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(record.getLastEventTimestamp()).isCloseTo(lastEventTimestamp1, toleratedInstantOffset);
        assertThat(record.getLastRecordUpdateTimestamp()).isCloseTo(now, within(1000, ChronoUnit.MILLIS));
    }

    //------------------------------------------------------------------------------------------------------------------

    private Mono<Integer> getMethod(String method, String idString, Instant jobAcceptedTimestamp,
                                    String processKey, String pausedBucketKey, String state, Instant lastEventTimestamp) {
        Mono<Integer> mono;
        if ("findAndUpdateOrInsert".equals(method)) {
            mono = stateRecordService.findAndUpdateOrInsert(idString, jobAcceptedTimestamp, processKey,
                state, lastEventTimestamp, pausedBucketKey);
        } else if ("insertUpdate".equals(method)) {
            mono = stateRecordService.insertUpdate(idString, jobAcceptedTimestamp, processKey,
                state, lastEventTimestamp, pausedBucketKey);
        } else if ("insertIgnoreConflictThenUpdate".equals(method)) {
            mono = stateRecordService.insertIgnoreConflictThenUpdate(idString, jobAcceptedTimestamp, processKey,
                state, lastEventTimestamp, pausedBucketKey);
        } else if ("insertOnConflictUpdate".equals(method)) {
            mono = stateRecordService.insertOnConflictUpdate(idString, jobAcceptedTimestamp, processKey,
                state, lastEventTimestamp, pausedBucketKey);
        } else if ("upsert".equals(method)) {
            mono = stateRecordService.upsert(idString, jobAcceptedTimestamp, processKey,
                state, lastEventTimestamp, pausedBucketKey);
        } else {
            throw new IllegalArgumentException("Invalid method \"" + method + "\"!");
        }
        return mono;
    }
}