package com.giraone.jobs.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// Same as @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_PATTERN, timezone = "UTC")
// but allows more control and tolerant deserializer also.
public class CustomInstantSerializer extends JsonSerializer<Instant> {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final DateTimeFormatter WRITE_FORMAT = DateTimeFormatter
        .ofPattern(DATE_TIME_PATTERN)
        .withZone(ZoneId.of(ZoneOffset.UTC.getId()));

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        if (value != null) {
            final String str = WRITE_FORMAT.format(value);
            gen.writeString(str);
        }
    }
}
