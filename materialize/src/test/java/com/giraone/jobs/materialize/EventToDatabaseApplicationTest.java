package com.giraone.jobs.materialize;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class EventToDatabaseApplicationTest {

    @Test
    void contextLoadsDoesNotThrow() {
        assertDoesNotThrow(() -> EventToDatabaseApplication.main(new String[]{}));
    }
}