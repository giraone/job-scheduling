package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.model.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.annotation.DirtiesContext;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class StateRecordServiceIntTest {

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
        stateRecordService.update(id.toString(), "NOTIFIED", notifyTimeStamp, null)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        // act - an older event
        stateRecordService.update(id.toString(), "COMPLETED", completedTimeStamp,  null)
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
}