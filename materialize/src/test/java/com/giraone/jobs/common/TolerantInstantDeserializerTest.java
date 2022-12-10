package com.giraone.jobs.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TolerantInstantDeserializerTest {

    private static final Instant CHECK = Instant.parse("2021-12-03T10:15:30.00Z");

    @ParameterizedTest
    @CsvSource({
        "2021-12-03T10:15:30.247Z",
        "2021-12-03T10:15:30.377302600Z",
        "2021-12-03T14:15:30+01:00",
        "2021-12-03T10:15:30",
        "2022-12-03T14:15:30,928228300+01:00"
    })
    void parse(String input) {

        Instant actual = TolerantInstantDeserializer.parse(input);
        assertThat(actual).isNotNull();
        assertThat(actual).isAfterOrEqualTo(CHECK);
    }
}
