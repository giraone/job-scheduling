package com.giraone.jobs.receiver.web.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import com.giraone.jobs.receiver.service.ProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

@RestController()
@RequestMapping(value = "/api")
public class JobReceiveResource {

    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private static final String METRICS_PREFIX = "receiver.jobs.received";
    private static final String METRICS_JOBS_VALUE_SUCCESS = "success";
    private static final String METRICS_JOBS_VALUE_FAILURE = "failure";

    private Counter successCounter;
    private Counter failureCounter;

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
        this.failureCounter = Counter.builder(METRICS_PREFIX + "." + METRICS_JOBS_VALUE_FAILURE)
            .description("Counter for all received jobs, that are successfully passed to Kafka.")
            .register(meterRegistry);
    }

    @GetMapping("/metrics")
    public Mono<Map<String, Object>> getCounter() {
        return this.producerService.getMetrics();
    }

    // @Timed(value = "receiver.jobs.time", description = "Time taken to pass job to Kafka")
    @Observed
    @PostMapping("/jobs")
    public Mono<ResponseEntity<Map<String, Object>>> create(@RequestBody Flux<ByteBuffer> body) {

        Mono<String> mono1 = deserialize(body)
            .flatMap(producerService::send);

        Mono<Boolean> mono2 = stopOnPatternDetection(body);

        Mono<Tuple2<String, Boolean>> zippedMono = mono1.zipWith(mono2);

        return zippedMono
            .map(Tuple2::getT1)
            .map(this::ok)
            .doOnSuccess(any -> successCounter.increment())
            .doOnError(any -> failureCounter.increment())
            .onErrorResume(PatternDetectedException.class, this::bad);
    }

    Mono<ResponseEntity<Map<String, Object>>> bad(PatternDetectedException patternDetectedException) {
        return Mono.just(
            ResponseEntity
                .badRequest()
                .body(Map.of("detection", true))
        );
    }

    ResponseEntity<Map<String, Object>> ok(String key) {
        return ResponseEntity
            .ok()
            .body(
                Map.of(
                    "key", key,
                    "detection", false
                )
            );
    }

    Mono<Boolean> stopOnPatternDetection(Flux<ByteBuffer> content) {
        return readContent(content)
            .map(bytes -> {
                if (patternIsDetected(bytes)) {
                    throw new PatternDetectedException("Pattern detected in " + Arrays.toString(bytes));
                }
                return true;
            });
    }

    Mono<Map<String, Object>> deserialize(Flux<ByteBuffer> content) {

        return readContent(content)
            .map(bytes -> {
                try {
                    return objectMapper.readValue(bytes, MAP);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    Mono<byte[]> readContent(Flux<ByteBuffer> content) {

        return content
            .reduce(
                new ByteArrayOutputStream(),
                (byteArrayOutputStream, buffer) -> {
                    byteArrayOutputStream.write(buffer.array(), 0, buffer.limit());
                    return byteArrayOutputStream;
                })
            .map(ByteArrayOutputStream::toByteArray);
    }

    private boolean patternIsDetected(byte[] content) {

        return new String(content).contains("PATTERN-X");
    }
}