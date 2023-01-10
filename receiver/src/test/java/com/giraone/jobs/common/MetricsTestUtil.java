package com.giraone.jobs.common;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTestUtil {

    private final WebTestClient webTestClient;

    public MetricsTestUtil(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    public Double counterExistsAndGet(String path) {
        Map<String, Object> metrics = webTestClient
            .get()
            .uri(path)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(MAP)
            .returnResult()
            .getResponseBody();

        // assert for metrics counter
        assertThat(metrics).isNotNull();
        assertThat(metrics.get("measurements")).isNotNull();
        assertThat(metrics.get("measurements")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> measurements = (List<Map<String, Object>>) metrics.get("measurements");
        assertThat(measurements.get(0).get("statistic")).isEqualTo("COUNT");
        assertThat(measurements.get(0).get("value")).isInstanceOf(Double.class);
        return (Double) measurements.get(0).get("value");
    }

    public void counterExistsAndIsGreaterThan(String path, double expected) {
        Double actual = counterExistsAndGet(path);
        assertThat(actual).isGreaterThan(expected);
    }
}