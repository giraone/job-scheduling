package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.model.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@DirtiesContext
class JobRecordServiceIntTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRecordServiceIntTest.class);

    @Autowired
    StateRecordService stateRecordService;

    @Test
    void saveAndFindAll() {

        // arrange
        Instant nowInstant = Instant.now();
        Instant eventTime1 = nowInstant.minusSeconds(20);
        Instant eventTime2 = nowInstant.minusSeconds(20);
        Tsid id1 = TsidCreator.getTsid256();
        Tsid id2 = TsidCreator.getTsid256();

        stateRecordService.insert(id1.toString(), eventTime1, nowInstant, "A01")
            .as(StepVerifier::create)
            .then(() -> LOGGER.info("stateRecord1 inserted"))
            .expectNextCount(1)
            .verifyComplete();

        stateRecordService.insert(id2.toString(), eventTime2, nowInstant, "A01")
            .as(StepVerifier::create)
            .then(() -> LOGGER.info("stateRecord2 inserted"))
            .expectNextCount(1)
            .verifyComplete();

        // act/assert
        stateRecordService.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, JobRecord.ATTRIBUTE_id)))
            .as(StepVerifier::create)
            .then(() -> LOGGER.info("findAll called"))
            .assertNext(stateRecord -> {
                Assertions.assertThat(stateRecord).isNotNull();
                Assertions.assertThat(stateRecord.getId()).isEqualTo(id2.toLong());
                Assertions.assertThat(stateRecord.getStatus()).isEqualTo(JobRecord.STATE_accepted);
                Assertions.assertThat(stateRecord.getJobAcceptedTimestamp()).isCloseTo(eventTime2, within(1, ChronoUnit.MILLIS));
                Assertions.assertThat(stateRecord.getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1000, ChronoUnit.MILLIS));
            })
            .assertNext(stateRecord -> {
                Assertions.assertThat(stateRecord).isNotNull();
                Assertions.assertThat(stateRecord.getId()).isEqualTo(id1.toLong());
                Assertions.assertThat(stateRecord.getStatus()).isEqualTo(JobRecord.STATE_accepted);
                Assertions.assertThat(stateRecord.getJobAcceptedTimestamp()).isCloseTo(eventTime1, within(1, ChronoUnit.MILLIS));
                Assertions.assertThat(stateRecord.getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1000, ChronoUnit.MILLIS));
            })
            .verifyComplete();
    }
}