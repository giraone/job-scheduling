package com.giraone.jobs.materialize.web.rest;

import com.giraone.jobs.materialize.persistence.JobRecord;
import com.giraone.jobs.materialize.service.StateRecordService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController()
@RequestMapping(value = "/api")
public class StateRecordResource {

    private final StateRecordService stateRecordService;

    public StateRecordResource(StateRecordService stateRecordService) {
        this.stateRecordService = stateRecordService;
    }

    @GetMapping("/state-records-count")
    public Mono<Long> countAll() {
        return this.stateRecordService.countAll();
    }

    @GetMapping("/state-records")
    public Flux<JobRecord> findAll(final Pageable pageable) {
        return this.stateRecordService.findAll(pageable);
    }
}