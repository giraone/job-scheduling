package com.giraone.jobs.materialize.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the @link {@link PingService} service using a full integration test.
 */
@SpringBootTest(classes = { PingService.class })
class PingServiceIntTest {

    @Autowired
    PingService pingService;

    @Test
    void assertThat_getOkString_works() {

        pingService.getOkString()
            .as(StepVerifier::create)
            .assertNext(ret -> assertThat(ret).isEqualTo("OK"))
            .verifyComplete();
    }
}