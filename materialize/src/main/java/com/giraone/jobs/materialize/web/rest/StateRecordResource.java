package com.giraone.jobs.materialize.web.rest;

import com.giraone.jobs.materialize.model.JobRecord;
import com.giraone.jobs.materialize.service.StateRecordService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController()
@RequestMapping(value = "/api")
public class StateRecordResource {

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 1_000, Sort.by(Sort.Order.asc(JobRecord.ATTRIBUTE_id)));

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