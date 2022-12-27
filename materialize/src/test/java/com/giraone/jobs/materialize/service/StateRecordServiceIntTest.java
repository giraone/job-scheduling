package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.model.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class StateRecordServiceIntTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateRecordServiceIntTest.class);

    @Autowired
    private StateRecordService stateRecordService;

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Test
    void assertThat_update_does_not_overwrite_newer_entries() {

        // arrange
        Tsid id = TsidCreator.getTsid256();
        Instant now = Instant.now();
        Instant createdTimeStamp = now.minusSeconds(10);
        Instant completedTimeStamp = now.minusSeconds(5);
        Instant notifyTimeStamp = now.minusSeconds(1);

        // arrange - insert
        stateRecordService.insert(id.toString(), createdTimeStamp, "V001")
            .as(StepVerifier::create)
            .verifyComplete();

        // act - a newer event
        stateRecordService.update(true, id.toString(), "NOTIFIED", notifyTimeStamp, null)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        // act - an older event
        stateRecordService.update(true, id.toString(), "COMPLETED", completedTimeStamp, null)
            .as(StepVerifier::create)
            .expectNext(0)
            .verifyComplete();

        // assert
        r2dbcEntityTemplate.select(JobRecord.class).count()
            .as(StepVerifier::create)
            .assertNext(count -> {
                assertThat(count).isEqualTo(1L);
            })
            .verifyComplete();
    }

    @Test
    void assertThat_findAndUpdateOrInsert_worksSingle() throws ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 2; i++) {
            Tsid id = TsidCreator.getTsid256();
            findAndUpdateOrInsert(executorService, id, "scheduled");
        }
    }

    @Test
    void assertThat_findAndUpdateOrInsert_worksConcurrent() throws ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            Tsid id = TsidCreator.getTsid256();
            findAndUpdateOrInsert(executorService, id, "scheduled", "completed");
        }
    }

    private void findAndUpdateOrInsert(ExecutorService executorService, Tsid id,
                                       String state1, String state2) throws ExecutionException, InterruptedException {

        // arrange
        String idString = id.toString();
        Instant now = Instant.now();
        Instant jobAcceptedTimestamp = now.minusSeconds(10);
        String processKey = "V001";
        String pausedBucketKey = null;

        Instant lastEventTimestamp1 = now.minusSeconds(2);
        Mono<Integer> mono1 = stateRecordService.findAndUpdateOrInsert(idString, jobAcceptedTimestamp, processKey,
            state1, lastEventTimestamp1, pausedBucketKey);
        Instant lastEventTimestamp2 = now.minusSeconds(1);
        Mono<Integer> mono2 = stateRecordService.findAndUpdateOrInsert(idString, jobAcceptedTimestamp, processKey,
            state2, lastEventTimestamp2, pausedBucketKey);

        // act
        Future<Integer> future1 = executorService.submit(() -> mono1.block());
        Future<Integer> future2 = executorService.submit(() -> mono2.block());

        // assert
        assertThat(future1).succeedsWithin(Duration.ofSeconds(1));
        assertThat(future2).succeedsWithin(Duration.ofSeconds(1));
        LOGGER.debug("Call for {}: {}", state1, future1.get());
        LOGGER.debug("Call for {}: {}", state2, future2.get());
        assertThat(future1.get()).isEqualTo(1);
        assertThat(future2.get()).isEqualTo(1);
    }

    // to test single execution
    private void findAndUpdateOrInsert(ExecutorService executorService, Tsid id,
                                       String state1) throws ExecutionException, InterruptedException {

        // arrange
        String idString = id.toString();
        Instant now = Instant.now();
        Instant jobAcceptedTimestamp = now.minusSeconds(10);
        String processKey = "V001";
        String pausedBucketKey = null;

        Instant lastEventTimestamp1 = now.minusSeconds(2);
        Mono<Integer> mono1 = stateRecordService.findAndUpdateOrInsert(idString, jobAcceptedTimestamp, processKey,
            state1, lastEventTimestamp1, pausedBucketKey);

        // act
        Future<Integer> future1 = executorService.submit(() -> mono1.block());

        // assert
        assertThat(future1).succeedsWithin(Duration.ofSeconds(1));
        LOGGER.debug("Call for {}: {}", state1, future1.get());
        assertThat(future1.get()).isEqualTo(1);
    }
}