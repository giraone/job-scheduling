package com.giraone.jobs.materialize.web.rest;

import com.giraone.jobs.materialize.persistence.JobRecord;
import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

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

    @BeforeEach
    void init() {
        r2dbcEntityTemplate.delete(JobRecord.class).all().block();
    }

    @Test
    void findAllWorks() {

        // arrange
        Instant nowInstant = Instant.now();
        long id1 = TsidCreator.getTsid256().toLong();
        long id2 = TsidCreator.getTsid256().toLong();
        JobRecord jobRecord1 = new JobRecord(id1, nowInstant, nowInstant, nowInstant, "accepted", null, 1L);
        JobRecord jobRecord2 = new JobRecord(id2, nowInstant, nowInstant, nowInstant, "scheduled", null, 1L);
        r2dbcEntityTemplate.insert(jobRecord2).block();
        r2dbcEntityTemplate.insert(jobRecord1).block();

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
        assertThat(list.get(0).getId()).isEqualTo(id1);
        assertThat(list.get(0).getStatus()).isEqualTo("accepted");
        assertThat(list.get(0).getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1, ChronoUnit.MILLIS));
        assertThat(list.get(1).getId()).isEqualTo(id2);
        assertThat(list.get(1).getStatus()).isEqualTo("scheduled");
        assertThat(list.get(1).getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void countAllWorks() {

        // arrange
        Instant nowInstant = Instant.now();
        long id1 = TsidCreator.getTsid256().toLong();
        long id2 = TsidCreator.getTsid256().toLong();
        JobRecord jobRecord1 = new JobRecord(id1, nowInstant, nowInstant, nowInstant, "accepted", null, 1L);
        JobRecord jobRecord2 = new JobRecord(id2, nowInstant, nowInstant, nowInstant, "scheduled", null, 1L);
        r2dbcEntityTemplate.insert(jobRecord2).subscribe();
        r2dbcEntityTemplate.insert(jobRecord1).subscribe();

        // act
        Long count = webTestClient
            .get()
            .uri("/api/state-records-count")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Long.class)
            .returnResult()
            .getResponseBody();

        // assert
        assertThat(count).isEqualTo(2);
    }
}