package com.giraone.jobs.common;

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
    // 2022-12-09T21:00:33.377302600Z
    private static final DateTimeFormatter TOLERANT_READ_FORMAT1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nX");
    // 2022-12-10T10:01:26+01:00
    private static final DateTimeFormatter TOLERANT_READ_FORMAT2 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    // 2011-12-03T10:15:30
    private static final DateTimeFormatter TOLERANT_READ_FORMAT3 = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        .withZone(ZoneId.of(ZoneOffset.UTC.getId()));
    // 2022-12-03T14:15:30,928228300+01:00
    private static final DateTimeFormatter TOLERANT_READ_FORMAT4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss','nX");

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext deserializationContext)
        throws IOException {

        final String text = parser.getText();
        return parse(text);
    }

    public static Instant parse(String text) {

        if (text == null) {
            return null;
        }
        final int len = text.length();
        if (len == 24) {
            return Instant.from(DEFAULT_READ_FORMAT.parse(text));
        } else if (len == 30) {
            return Instant.from(TOLERANT_READ_FORMAT1.parse(text));
        } else if (len == 25) {
            return Instant.from(TOLERANT_READ_FORMAT2.parse(text));
        } else if (len == 19) {
            return Instant.from(TOLERANT_READ_FORMAT3.parse(text));
        } else if (len == 35) {
            return Instant.from(TOLERANT_READ_FORMAT4.parse(text));
        } else {
            return Instant.parse(text);
        }
    }
}
