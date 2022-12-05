package com.giraone.jobs.receiver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class ReceiverApplicationTest {

    @Test
    void contextLoadsDoesNotThrow() {
        assertDoesNotThrow(() -> ReceiverApplication.main(new String[]{}));
    }
}