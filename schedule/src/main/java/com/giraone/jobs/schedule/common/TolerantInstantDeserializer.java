package com.giraone.jobs.schedule.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TolerantInstantDeserializer extends JsonDeserializer<Instant> {

    // 2011-12-03T10:15:30.247Z
    private static final DateTimeFormatter DEFAULT_READ_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    // 2011-12-03T10:15:30
    private static final DateTimeFormatter TOLERANT_READ_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        .withZone(ZoneId.of(ZoneOffset.UTC.getId()));

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext deserializationContext)
        throws IOException {

        final String text = parser.getText();
        if (text == null) {
            return null;
        }
        if (text.length() == 24) {
            return Instant.from(DEFAULT_READ_FORMAT.parse(text));
        } else {
            return Instant.from(TOLERANT_READ_FORMAT.parse(text));
        }
    }
}
