package com.giraone.jobs.schedule.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test the actuator end points.
 * TODO: more tests needed!
 */
@SpringBootTest
@AutoConfigureWebTestClient
// This is needed in SCS with Kafka Binder - otherwise one get Application Context error
// with "Multiple functions found, but function definition property is not set."
@ActiveProfiles({"test"})
class ActuatorIntTest {

    @Autowired
    private WebTestClient webTestClient;

    @DisplayName("Test GET /actuator/health")
    @Test
    @Disabled
        // TODO: Beim Testen aktuell "DOWN"
    void testThat_actuator_health_isUp() {

        // act/assert
        webTestClient.get().uri("/actuator/health")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/vnd.spring-boot.actuator.v3+json")
            .expectBody()
            .jsonPath("$.status").isNotEmpty()
            .jsonPath("$.status").isEqualTo("UP")
        ;
    }

    @DisplayName("Test GET /actuator/info")
    @Test
    void testThat_actuator_info_IsOk() {

        // act/assert
        webTestClient.get().uri("/actuator/info")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk();
    }
}
