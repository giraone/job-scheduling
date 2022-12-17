package com.giraone.jobs.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Paused / Running
 */
public enum ActivationEnum {
    @JsonProperty("ACTIVE")
    ACTIVE ("ACTIVE"),
    @JsonProperty("PAUSED")
    PAUSED ("PAUSED");

    private static final Map<String, ActivationEnum> FORMAT_MAP = Stream
        .of(ActivationEnum.values())
        .collect(Collectors.toMap(s -> s.label, Function.identity()));

    public final String label;

    ActivationEnum(String label) {
        this.label = label;
    }

    @JsonCreator // This is the factory method and must be static
    public static ActivationEnum fromString(String string) {
        return Optional.ofNullable(FORMAT_MAP.get(string)).orElseThrow(() -> new IllegalArgumentException(string));
    }

    @Override
    public String toString() {
        return label;
    }
}
