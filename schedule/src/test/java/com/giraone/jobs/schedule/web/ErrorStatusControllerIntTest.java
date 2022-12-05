package com.giraone.jobs.schedule.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test class for the ErrorStatusController REST controller.
 * To show whether the "dump" works.
 *
 * @see ErrorStatusController
 */
@SpringBootTest
@DirtiesContext
@AutoConfigureWebTestClient
// This is needed in SCS with Kafka Binder - otherwise one get Application Context error
// with "Multiple functions found, but function definition property is not set."
@ActiveProfiles({"test", "processSchedule"})
@SuppressWarnings("squid:S100") // Method naming
class ErrorStatusControllerIntTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void assertThat_endpoint_works() {

        // act/assert
        webTestClient.get().uri("/admin-api/error-status/processSchedule")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON) // Normally not necessary
            .expectBody()
            .jsonPath("$.success_total").isEqualTo(0)
            .jsonPath("$.error_total").isEqualTo(0);
    }
}
