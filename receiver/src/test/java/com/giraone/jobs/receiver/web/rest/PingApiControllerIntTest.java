package com.giraone.jobs.receiver.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test class for the {@link PingApiController} REST controller.
 * Basically only to show, how these kinds of reactive WebTestClient tests with "full application context" work.
 */
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "30000") // 30 seconds
class PingApiControllerIntTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void pingReturnsStatus() {

        // act/assert
        webTestClient.get().uri("/api/ping")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON) // Normally not necessary
            .expectBody()
            .jsonPath("$.status").isNotEmpty()
            .jsonPath("$.status").isEqualTo("OK");
    }
}
