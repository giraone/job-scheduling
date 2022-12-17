package com.giraone.jobs.receiver.web.rest;

import com.giraone.jobs.receiver.service.ProducerService;
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

    private final ProducerService producerService;

    public JobReceiveResource(ProducerService producerService) {
        this.producerService = producerService;
    }

    @GetMapping("/metrics")
    public Mono<Map<String, Object>> getCounter() {
        return this.producerService.getMetrics();
    }

    @PostMapping("/jobs")
    public Mono<Map<String, Object>> create(@RequestBody Map<String, Object> event) {
        return this.producerService.send(event)
            .map(key -> Map.of("key", key));
    }
}