package com.giraone.jobs.receiver.web.rest;

import com.giraone.jobs.common.MetricsTestUtil;
import com.giraone.jobs.receiver.config.ApplicationProperties;
import com.giraone.jobs.receiver.service.ProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test the @link {@link JobReceiveResource} controller using a web integration test without Kafka.
 */
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "30000") // 30 seconds
public class JobReceiveResourceTest {

    @MockBean
    private ProducerService producerService;

    @Autowired
    ApplicationProperties applicationProperties;
    @Autowired
    WebTestClient webTestClient;

    @Test
    void addJobFailsCorrectly_whenKafkaBrokerDoesNotAnswer() {

        // arrange
        MetricsTestUtil metricsTestUtil = new MetricsTestUtil(webTestClient);
        Double failures = metricsTestUtil.counterExistsAndGet("/actuator/metrics/receiver.jobs.received.failure");

        when(producerService.send(any()))
            .thenReturn(Mono.error(new org.apache.kafka.common.errors.TimeoutException("Kafka Test Timeout")));

        Instant now = Instant.now();
        String id = "JobReceiveResourceIntTest-" + System.nanoTime();
        Map<String, Object> body = Map.of(
            "requesterId", id,
            "eventTimestamp", DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()).format(now)
        );

        // act/assert
        String responseBody = webTestClient
            .post()
            .uri("/api/jobs")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        ;

        assertThat(responseBody).isEqualTo("Kafka error. Retry later.");

        // assert metrics counter
        metricsTestUtil.counterExistsAndIsGreaterThan("/actuator/metrics/receiver.jobs.received.failure", failures);
    }
}