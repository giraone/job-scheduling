package com.giraone.jobs.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum for Job-Status
 */
public enum JobStatusEnum {
    @JsonProperty("ACCEPTED")
    ACCEPTED("ACCEPTED"),
    @JsonProperty("SCHEDULED")
    SCHEDULED("SCHEDULED"),
    @JsonProperty("PAUSED")
    PAUSED("PAUSED"),
    @JsonProperty("FAILED")
    FAILED("FAILED"),
    @JsonProperty("COMPLETED")
    COMPLETED("COMPLETED"),
    @JsonProperty("NOTIFIED")
    NOTIFIED("NOTIFIED"),
    @JsonProperty("DELIVERED")
    DELIVERED("DELIVERED");

    private static Map<String, JobStatusEnum> FORMAT_MAP = Stream
        .of(JobStatusEnum.values())
        .collect(Collectors.toMap(s -> s.label, Function.identity()));

    public final String label;

    private JobStatusEnum(String label) {
        this.label = label;
    }

    @JsonCreator // This is the factory method and must be static
    public static JobStatusEnum fromString(String string) {
        return Optional.ofNullable(FORMAT_MAP.get(string)).orElseThrow(() -> new IllegalArgumentException(string));
    }
}
