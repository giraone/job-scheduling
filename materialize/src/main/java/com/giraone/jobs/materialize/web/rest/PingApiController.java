package com.giraone.jobs.materialize.web.rest;

import com.giraone.jobs.materialize.service.PingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for simple REST test.
 * The controller exists only to enable a smoke test for the Spring Boot configuration.
 */
@RestController
@RequestMapping("/api")
public class PingApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingApiController.class);

    private final PingService pingService;

    public PingApiController(PingService pingService) {
        this.pingService = pingService;
    }

    /**
     * GET  /api/ping : Simple endpoint ping method to check, whether application (Spring Controller Application Context) works
     *
     * @return the ResponseEntity with status 200 (OK) and { "status"; "OK" } as body data
     */
    @GetMapping("/ping")
    public Mono<ResponseEntity<Map<String, String>>> getPingStatus() {

        return pingService.getOkString()
            .doOnNext(value -> LOGGER.info("PingApiResource.getPingStatus called ret={}", value))
            .map(value -> ResponseEntity.ok(Map.of("status", value)));
    }
}
