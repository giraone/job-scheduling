package com.giraone.jobs.materialize.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ObjectMapperBuilder {

    // Hide
    private ObjectMapperBuilder() {
    }

    public static ObjectMapper build(boolean snakeCase, boolean sortKeys) {

        final ObjectMapper mapper = new ObjectMapper();
        // Ignore unknown properties in input
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Do not write empty/null values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Date/Date-Time settings
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // StdDateFormat is ISO8601 since jackson 2.9 - we force +05:00 instead of +0500
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        // Enum settings
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        if (snakeCase) {
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        }
        if (sortKeys) {
            mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        }
        return mapper;
    }
}
