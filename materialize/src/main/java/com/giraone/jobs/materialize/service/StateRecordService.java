package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.model.JobRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Service
public class StateRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateRecordService.class);

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public StateRecordService(R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    public Mono<JobRecord> insert(String id, Instant creationEventTimestamp, Instant now, String processKey) {

        // TODO: processKey ==> processId by DB query or DB view
        final long processId = Long.parseLong(processKey.substring(1), 10);
        final JobRecord jobRecord = new JobRecord(id, creationEventTimestamp, now /*X*/, processId);
        jobRecord.setLastRecordUpdateTimestamp(Instant.now());
        return r2dbcEntityTemplate.insert(jobRecord);
    }

    public Mono<Integer> update(String id, String state, Instant lastEventTimestamp, Instant now, String pausedBucketKey) {

        final Update update = Update.update(JobRecord.ATTRIBUTE_status, state)
            .set(JobRecord.ATTRIBUTE_lastEventTimestamp, lastEventTimestamp)
            .set(JobRecord.ATTRIBUTE_lastRecordUpdateTimestamp, now)
            .set(JobRecord.ATTRIBUTE_pausedBucketKey, pausedBucketKey);
        return r2dbcEntityTemplate
            .update(JobRecord.class)
            .matching(Query.query(
                    where(JobRecord.ATTRIBUTE_id).is(id))
                // TODO: and lastModificationTimestamp < event.eventTimestamp /*X*/
            )
            .apply(update)
            .doOnNext(updateCount -> LOGGER.debug("Update id={} state={} updated {} rows", id, state, updateCount));
    }

    public Mono<Long> countAll() {

        return r2dbcEntityTemplate.count(query(Criteria.empty()), JobRecord.class);
    }

    public Flux<JobRecord> findAll(Pageable pageable) {

        Pageable pageableWithDefault = pageable;
        if (pageable.getSort().isUnsorted()) {
            List<Sort.Order> orders = new ArrayList<>();
            orders.add(Sort.Order.asc(JobRecord.ATTRIBUTE_id));
            pageableWithDefault = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), by(orders));
        }
        return r2dbcEntityTemplate.select(query(Criteria.empty()).with(pageableWithDefault), JobRecord.class);
    }
}
