package com.giraone.jobs.schedule.web;

import com.giraone.jobs.schedule.stopper.SwitchOnOff;
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
public class RunningController {

    public static final String ATTRIBUTE_running = "running";
    public static final String ATTRIBUTE_paused = "paused";

    private final SwitchOnOff switchOnOff;

    public RunningController(SwitchOnOff switchOnOff) {
        this.switchOnOff = switchOnOff;
    }

    @GetMapping("/processors/{processorName}/resume")
    public Mono<ResponseEntity<Map<String, Boolean>>> resume(@PathVariable String processorName) {

        log.debug("RunningController.start {} called", processorName);
        return Mono
            .just(Boolean.FALSE)
            .map(value -> switchOnOff.changeStateToPaused(processorName, value))
            .map(value -> ResponseEntity.ok(Map.of(ATTRIBUTE_paused, value)));
    }

    @GetMapping("/processors/{processorName}/pause")
    public Mono<ResponseEntity<Map<String, Boolean>>> pause(@PathVariable String processorName) {

        log.debug("RunningController.stop {} called", processorName);
        return Mono
            .just(Boolean.TRUE)
            .map(value -> switchOnOff.changeStateToPaused(processorName, value))
            .map(value -> ResponseEntity.ok(Map.of(ATTRIBUTE_paused, value)));
    }

    @GetMapping("/processors/{processorName}/status")
    public Mono<ResponseEntity<Map<String, Boolean>>> status(@PathVariable String processorName) {

        log.debug("RunningController.status {} called", processorName);
        boolean running = switchOnOff.isRunning(processorName);
        boolean paused = switchOnOff.isPaused(processorName);
        return Mono.just(ResponseEntity.ok(
            Map.of(
                ATTRIBUTE_running, running,
                ATTRIBUTE_paused, paused
            )));
    }
}
