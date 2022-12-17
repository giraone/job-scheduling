package com.giraone.jobs.materialize.web.rest;

import com.giraone.jobs.materialize.model.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Test the @link {@link StateRecordResource} controller using a full integration test.
 */
@SpringBootTest
@DirtiesContext
@AutoConfigureWebTestClient(timeout = "30000") // 30 seconds
class JobRecordResourceIntTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    R2dbcEntityTemplate r2dbcEntityTemplate;

    @Test
    void findAllWorks() {

        // arrange
        Instant nowInstant = Instant.now();
        Tsid tsid1 = TsidCreator.getTsid256();
        Tsid tsid2 = TsidCreator.getTsid256();
        JobRecord jobRecord1 = new JobRecord(tsid1.toLong(), nowInstant, nowInstant, nowInstant, "accepted", 1L);
        JobRecord jobRecord2 = new JobRecord(tsid2.toLong(), nowInstant, nowInstant, nowInstant, "scheduled", 1L);
        r2dbcEntityTemplate.insert(jobRecord2).subscribe();
        r2dbcEntityTemplate.insert(jobRecord1).subscribe();

        // act
        List<JobRecord> list = webTestClient
            .get()
            .uri("/api/state-records")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(JobRecord.class)
            .returnResult()
            .getResponseBody();

        // assert
        assertThat(list).isNotNull();
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(tsid1.toString());
        assertThat(list.get(0).getStatus()).isEqualTo("accepted");
        assertThat(list.get(0).getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1, ChronoUnit.MILLIS));
        assertThat(list.get(1).getId()).isEqualTo(tsid2.toString());
        assertThat(list.get(1).getStatus()).isEqualTo("scheduled");
        assertThat(list.get(1).getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1, ChronoUnit.MILLIS));
    }
}