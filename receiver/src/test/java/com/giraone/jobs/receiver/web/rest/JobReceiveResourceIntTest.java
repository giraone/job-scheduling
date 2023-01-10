package com.giraone.jobs.receiver.web.rest;

import com.giraone.jobs.common.MetricsTestUtil;
import com.giraone.jobs.receiver.config.ApplicationProperties;
import com.giraone.jobs.receiver.service.AbstractKafkaIntTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the @link {@link JobReceiveResource} controller using a full integration test.
 * See also <a href="https://github.com/reactor/reactor-kafka/blob/main/src/test/java/reactor/kafka/sender/KafkaSenderTest.java">KafkaSenderTest.java</a>
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // because init() needs ConsumerService
@AutoConfigureWebTestClient(timeout = "30000") // 30 seconds
public class JobReceiveResourceIntTest extends AbstractKafkaIntTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobReceiveResourceIntTest.class);

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    @Autowired
    ApplicationProperties applicationProperties;
    @Autowired
    WebTestClient webTestClient;

    private Consumer<String, String> consumer;

    @BeforeEach
    public void setUp() {
        String topic = applicationProperties.getJobAcceptedTopic();
        createNewTopic(topic);
        consumer = createConsumer(topic);
        LOGGER.info("Consumer for \"{}\" created.", topic);
    }

    @AfterEach
    public void tearDown() {
        if (consumer != null) {
            consumer.close();
            LOGGER.info("Consumer for \"{}\" closed.", applicationProperties.getJobAcceptedTopic());
        }
    }

    @Test
    void metricsWorks() {

        Map<String, Object> metrics = webTestClient
            .get()
            .uri("/api/metrics")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(MAP)
            .returnResult()
            .getResponseBody();

        // assert
        assertThat(metrics).isNotNull();
        assertThat(metrics.get("sent")).isNotNull();
        assertThat(metrics.get("failed")).isNotNull();
    }

    @Test
    void addJobWorks() {

        // arrange
        MetricsTestUtil metricsTestUtil = new MetricsTestUtil(webTestClient);
        Double success = metricsTestUtil.counterExistsAndGet("/actuator/metrics/receiver.jobs.received.success");

        Instant now = Instant.now();
        String id = "JobReceiveResourceIntTest-" + System.nanoTime();
        Map<String, Object> body = Map.of(
            "requesterId", id,
            "eventTimestamp", DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()).format(now)
        );

        // act
        Map<String, Object> response = webTestClient
            .post()
            .uri("/api/jobs")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(MAP)
            .returnResult()
            .getResponseBody();

        // assert
        assertThat(response).isNotNull();
        assertThat(response.get("key")).isNotNull();

        LOGGER.debug("Got message key \"{}\".", response.get("key"));

        waitForMessages(consumer, 1);
        receivedRecords.forEach(l -> {
            l.forEach(record -> {
                assertThat(record.key()).isNotNull();
                assertThat(record.value()).contains(id);
            });
        });

        // assert metrics counter
        metricsTestUtil.counterExistsAndIsGreaterThan("/actuator/metrics/receiver.jobs.received.success", success);
    }
}