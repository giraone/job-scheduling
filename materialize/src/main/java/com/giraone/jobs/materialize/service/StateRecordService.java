package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.model.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
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
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.giraone.jobs.materialize.model.JobRecord.STATE_accepted;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Service
@Transactional
public class StateRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateRecordService.class);

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public StateRecordService(R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    public Mono<JobRecord> insert(String idString, Instant jobAcceptedTimestamp, String processKey) {

        final long id = Tsid.from(idString).toLong();
        return insert(id, jobAcceptedTimestamp, processKey, STATE_accepted, Instant.now(), null);
    }

    public Mono<Integer> update(boolean checkTimestamp, String idString, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        return update(checkTimestamp, id, state, lastEventTimestamp, pausedBucketKey);
    }

    // Solution 1 - findById, then update or insert
    public Mono<Integer> findAndUpdateOrInsert(String idString, Instant jobAcceptedTimestamp, String processKey, String state,
                                               Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        return findById(id)
            .flatMap(found -> update(true, id, state, lastEventTimestamp, pausedBucketKey))
            .switchIfEmpty(
                insert(id, jobAcceptedTimestamp, processKey, state, lastEventTimestamp, pausedBucketKey).map(j -> 1)
            );
    }

    // Solution 2 - upsert = update, if update count = 0 or exception, then insert
    public Mono<Integer> upsert(String idString, Instant jobAcceptedTimestamp, String processKey, String state,
                                Instant lastEventTimestamp, String pausedBucketKey) {

        return update(false, idString, state, lastEventTimestamp, pausedBucketKey)
            .doOnError(exception -> {
                LOGGER.warn("UPDATE failed: {}", exception.getMessage());
            })
            .onErrorReturn(0)
            .flatMap(x -> {
                if (x == 0) {
                    LOGGER.warn("UPDATE failed! Trying INSERT.");
                    return insert(idString, jobAcceptedTimestamp, processKey).map(jobRecord -> 1);
                } else {
                    LOGGER.debug("UPDATE succeeded.");
                    return Mono.just(1);
                }
            });
    }

    // Solution 3 - insertUpdate = insert, if duplicate key exception, then update
    public Mono<Integer> insertUpdate(String idString, Instant jobAcceptedTimestamp, String processKey, String state,
                                      Instant lastEventTimestamp, String pausedBucketKey) {

        return insert(idString, jobAcceptedTimestamp, processKey)
            .map(jobRecordStored -> 1)
            .onErrorResume(R2dbcDataIntegrityViolationException.class, (exception) -> {
                LOGGER.warn("INSERT failed! Trying UPDATE. Exception = {}", exception.getMessage());
                return update(false, idString, state, lastEventTimestamp, pausedBucketKey);
            });
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

    //------------------------------------------------------------------------------------------------------------------

    private Mono<JobRecord> insert(long id, Instant jobAcceptedTimestamp, String processKey, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        // TODO: processKey ==> processId by DB query or DB view
        final long processId = 1000L + Long.parseLong(processKey.substring(1), 10);
        final JobRecord jobRecord = new JobRecord(id, jobAcceptedTimestamp, lastEventTimestamp, processId);
        jobRecord.setStatus(state);
        jobRecord.setPausedBucketKey(pausedBucketKey);
        jobRecord.setLastRecordUpdateTimestamp(Instant.now());
        return r2dbcEntityTemplate.insert(jobRecord);
    }

    /**
     * Update an existing JobRecord and set its state, lastEventTimestamp and pausedBucketKey
     * with the given values and update the lastRecordUpdateTimestamp with Instant.now().
     *
     * @param checkTimestamp if true, check the lastEventTimestamp and do not perform an update, if the
     * lastEventTimestamp of the database record is already newer than the given on.
     * @param id the job record id
     * @param state the state to be set
     * @param lastEventTimestamp the lastEventTimestamp to be set
     * @return A Mono of 1 (update was made), 0 (no update was made) or an error
     */
    private Mono<Integer> update(boolean checkTimestamp, long id, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        LOGGER.debug("update {}", id);
        final Instant lastRecordUpdateTimestamp = Instant.now();
        final Update update = Update
            .update(JobRecord.ATTRIBUTE_status, state)
            .set(JobRecord.ATTRIBUTE_lastEventTimestamp, lastEventTimestamp)
            .set(JobRecord.ATTRIBUTE_lastRecordUpdateTimestamp, lastRecordUpdateTimestamp)
            .set(JobRecord.ATTRIBUTE_pausedBucketKey, pausedBucketKey);
        Criteria criteria = where(JobRecord.ATTRIBUTE_id).is(id);
        if (checkTimestamp) {
            criteria = criteria.and(JobRecord.ATTRIBUTE_lastEventTimestamp).lessThan(lastEventTimestamp);
        }
        return r2dbcEntityTemplate
            .update(JobRecord.class)
            .matching(Query.query(criteria))
            .apply(update)
            .doOnNext(updateCount -> LOGGER.debug("Update id={} state={} updated {} rows", id, state, updateCount));
    }

    /**
     * Query JobRecord by its id
     *
     * @param id the job record id
     * @return A Mono of the found JobRecord or an empty Mono
     */
    private Mono<JobRecord> findById(long id) {

        LOGGER.debug("findById {}", id);
        return r2dbcEntityTemplate
            .select(Query.query(where(JobRecord.ATTRIBUTE_id).is(id)), JobRecord.class)
            .next()
            .doOnNext(jobRecord -> LOGGER.debug("findById id={} state={}", id, jobRecord.getStatus()))
            ;
    }
}
