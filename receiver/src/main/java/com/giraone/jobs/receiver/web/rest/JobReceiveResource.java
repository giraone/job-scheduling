package com.giraone.jobs.receiver.web.rest;

import com.giraone.jobs.receiver.service.ProducerService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController()
@RequestMapping(value = "/api")
public class JobReceiveResource {

    private static final String METRICS_PREFIX = "receiver.jobs.received";
    private static final String METRICS_JOBS_VALUE_SUCCESS = "success";
    private static final String METRICS_JOBS_VALUE_FAILURE = "failure";

    private Counter successCounter;
    private Counter errorCounter;

    private final ProducerService producerService;
    private final MeterRegistry meterRegistry;

    public JobReceiveResource(ProducerService producerService, MeterRegistry meterRegistry) {
        this.producerService = producerService;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    private void init() {
        this.successCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_VALUE_SUCCESS)
            .description("Counter for all received jobs, that are successfully passed to Kafka.")
            .register(meterRegistry);
        this.errorCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_VALUE_FAILURE)
            .description("Counter for all received jobs, that are successfully passed to Kafka.")
            .register(meterRegistry);
    }

    @GetMapping("/metrics")
    public Mono<Map<String, Object>> getCounter() {
        return this.producerService.getMetrics();
    }

    @Timed(value = "receiver.jobs.time", description = "Time taken to pass job to Kafka")
    @PostMapping("/jobs")
    public Mono<Map<String, String>> create(@RequestBody Map<String, Object> event) {
        return this.producerService.send(event)
            .map(key -> Map.of("key", key))
            .doOnSuccess(any -> successCounter.increment())
            .doOnError(any -> errorCounter.increment())
            ;
    }
}