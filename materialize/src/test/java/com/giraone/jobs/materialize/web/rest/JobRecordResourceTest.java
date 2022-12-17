package com.giraone.jobs.materialize.web.rest;

import com.giraone.jobs.materialize.model.JobRecord;
import com.giraone.jobs.materialize.service.StateRecordService;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test the @link {@link StateRecordResource} controller using a mock for {@link StateRecordService}.
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = StateRecordResource.class)
@Import({StateRecordService.class})
@AutoConfigureWebTestClient(timeout = "30000") // 30 seconds
class JobRecordResourceTest {

    @MockBean
    StateRecordService stateRecordService;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void countAllWorks() {

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
        assertThat(count).isNotNull();
    }

    @Test
    void findAllWorks() {

        // arrange
        Instant nowInstant = Instant.now();
        Tsid tsid1 = TsidCreator.getTsid256();
        Tsid tsid2 = TsidCreator.getTsid256();
        JobRecord jobRecord1 = new JobRecord(tsid1.toLong(), nowInstant, nowInstant, nowInstant, JobRecord.STATE_accepted, 1L);
        JobRecord jobRecord2 = new JobRecord(tsid2.toLong(), nowInstant, nowInstant, nowInstant, JobRecord.STATE_scheduled,1L);
        Mockito.when(stateRecordService.findAll(any())).thenReturn(
            Flux.just(jobRecord1, jobRecord2));

        // act
        List<JobRecord> list = webTestClient
            .get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/api/state-records")
                    .queryParam("page", 0, "size", 10, "sort", "ID,ASC")
                    .build())
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
        assertThat(list.get(0).getStatus()).isEqualTo(JobRecord.STATE_accepted);
        assertThat(list.get(0).getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1, ChronoUnit.MILLIS));
        assertThat(list.get(1).getId()).isEqualTo(tsid2.toString());
        assertThat(list.get(1).getStatus()).isEqualTo(JobRecord.STATE_scheduled);
        assertThat(list.get(1).getLastRecordUpdateTimestamp()).isCloseTo(nowInstant, within(1, ChronoUnit.MILLIS));
    }
}