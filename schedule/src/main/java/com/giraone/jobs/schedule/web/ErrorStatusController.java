package com.giraone.jobs.schedule.web;

import com.giraone.jobs.schedule.processor.EventProcessor;
import com.giraone.jobs.schedule.stopper.ProcessingStopper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/admin-api")
@Slf4j
public class ErrorStatusController {

    /**
     * GET /admin-api/error-status/{process} : method to return the state of success/failures
     */
    @GetMapping("/error-status/{process}")
    public Mono<ResponseEntity<Map<String, Object>>> getErrorStatus(@PathVariable String process) {

        log.debug("ErrorStatusController.getErrorStatus called");
        final ProcessingStopper processingStopper = EventProcessor.STOPPER.get(process);
        if (processingStopper == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        final Map<String, Object> status = processingStopper.getStatus();
        return Mono.just(ResponseEntity.ok(status));
    }

    /**
     * GET /admin-api/reset-status/{process} : method to return the state of success/failures
     */
    @GetMapping("/reset-status/{process}")
    public Mono<ResponseEntity<Map<String, Object>>> resetErrorStatus(@PathVariable String process) {

        log.debug("ErrorStatusController.resetErrorStatus called");
        final ProcessingStopper processingStopper = EventProcessor.STOPPER.get(process);
        if (processingStopper == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        processingStopper.reset();
        final Map<String, Object> status = processingStopper.getStatus();
        return Mono.just(ResponseEntity.ok(status));
    }
}
