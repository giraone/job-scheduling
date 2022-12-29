package com.giraone.jobs.materialize.service;

import com.giraone.jobs.materialize.persistence.JobRecord;
import com.giraone.jobs.materialize.persistence.JobRecordRepository;
import com.github.f4b6a3.tsid.Tsid;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.giraone.jobs.materialize.persistence.JobRecord.STATE_accepted;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Service
@Transactional
public class StateRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateRecordService.class);

    private static final String METRICS_PREFIX = "materialize";
    private static final String METRICS_JOBS_LATENCY = "jobs.latency";
    private static final Integer PERCENTILE_PRECISION = 2;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final JobRecordRepository jobRecordRepository;
    private final MeterRegistry meterRegistry;

    private Timer latencyTimer;

    public StateRecordService(R2dbcEntityTemplate r2dbcEntityTemplate,
                              JobRecordRepository jobRecordRepository,
                              MeterRegistry meterRegistry) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.jobRecordRepository = jobRecordRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    private void init() {
        this.latencyTimer = Timer.builder(METRICS_PREFIX + "." + METRICS_JOBS_LATENCY)
            .publishPercentileHistogram()
            .percentilePrecision(PERCENTILE_PRECISION)
            .description("Measurement of the materialize latency in milliseconds")
            .register(meterRegistry);
    }

    //- INSERT ---------------------------------------------------------------------------------------------------------

    public Mono<JobRecord> insertAccepted(String idString, Instant jobAcceptedTimestamp, String processKey) {

        return insertFull(Tsid.from(idString).toLong(), jobAcceptedTimestamp, processKey, STATE_accepted, jobAcceptedTimestamp, null);
    }

    public Mono<Integer> insertAcceptedIgnoreConflict(String idString, Instant jobAcceptedTimestamp, String processKey) {

        return insertIgnoreConflict(Tsid.from(idString).toLong(), jobAcceptedTimestamp, processKey, STATE_accepted, jobAcceptedTimestamp, null);
    }

    //- UPDATE ---------------------------------------------------------------------------------------------------------

    // Solution 1 - findByIdForUpdate, then update or insert.
    public Mono<Integer> findAndUpdateOrInsert(
        String idString, Instant jobAcceptedTimestamp, String processKey, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        return findByIdForUpdate(id)
            .flatMap(found ->
                update(true, id, state, lastEventTimestamp, pausedBucketKey))
            .switchIfEmpty(
                insertFull(id, jobAcceptedTimestamp, processKey, state, lastEventTimestamp, pausedBucketKey).map(j -> 1)
            );
    }

    // Solution 2 - insertUpdate = insert, if duplicate key exception, then update with timestamp checking
    public Mono<Integer> insertUpdate(
        String idString, Instant jobAcceptedTimestamp, String processKey, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        return insertFull(id, jobAcceptedTimestamp, processKey, state, lastEventTimestamp, pausedBucketKey)
            .doOnError(throwable -> {
                LOGGER.debug("INSERT failed! {} {}", throwable.getClass(), throwable.getMessage());
            })
            .map(jobRecordStored -> 1)
            .onErrorReturn(DataIntegrityViolationException.class, 0)
            .flatMap(count -> {
                if (count == 0) {
                    LOGGER.info("INSERT failed! Trying UPDATE.");
                    return update(true, id, state, lastEventTimestamp, pausedBucketKey);
                } else {
                    return Mono.just(1);
                }
            });
    }

    // Solution 3 - insertIgnoreConflictThenUpdate = INSERT for each UPDATE ignoring errors, then UPDATE with timestamp checking
    public Mono<Integer> insertIgnoreConflictThenUpdate(
        String idString, Instant jobAcceptedTimestamp, String processKey, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        return insertIgnoreConflict(id, jobAcceptedTimestamp, processKey, state, lastEventTimestamp, pausedBucketKey)
            .doOnError(throwable -> {
                LOGGER.warn("INSERT ON CONFLICT DO NOTHING failed! {} {}", throwable.getClass(), throwable.getMessage());
            })
            .flatMap(count -> {
                if (count == 0) {
                    // This is the "normal" situation, when then INSERT arrived before the UPDATE
                    return update(true, id, state, lastEventTimestamp, pausedBucketKey);
                } else {
                    // This is the "exceptional" situation, when then INSERT has not yet arrived
                    LOGGER.info("INSERT for UPDATE has not yet arrived! id={}, state={}, lastEventTimestamp={}",
                        idString, state, lastEventTimestamp);
                    return Mono.just(1);
                }
            });
    }

    // Solution 4 - insertOnConflictUpdate
    public Mono<Integer> insertOnConflictUpdate(
        String idString, Instant jobAcceptedTimestamp, String processKey, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        // TODO: processKey ==> processId by DB query or DB view
        final long processId = Long.parseLong(processKey.substring(1), 10);
        final Instant lastRecordUpdateTimestamp = Instant.now();
        final long latency = lastRecordUpdateTimestamp.toEpochMilli() - lastEventTimestamp.toEpochMilli();
        this.latencyTimer.record(latency, TimeUnit.MILLISECONDS);
        return jobRecordRepository.insertOnConflictUpdateAndCheckTime(id, jobAcceptedTimestamp, lastEventTimestamp,
                lastRecordUpdateTimestamp, state, pausedBucketKey, processId)
            .doOnError(throwable -> {
                LOGGER.debug("INSERT ON CONFLICT UPDATE failed! {} {}", throwable.getClass(), throwable.getMessage());
            });
    }

    // Solution 5 - upsert = update, if update count = 0 or exception, then insert
    public Mono<Integer> upsert(
        String idString, Instant jobAcceptedTimestamp, String processKey, String state, Instant lastEventTimestamp, String pausedBucketKey) {

        final long id = Tsid.from(idString).toLong();
        return update(false, id, state, lastEventTimestamp, pausedBucketKey)
            .doOnError(throwable -> {
                LOGGER.debug("UPDATE failed: Exception = {}", throwable.getMessage());
            })
            .onErrorReturn(0)
            .flatMap(count -> {
                if (count == 0) {
                    LOGGER.debug("UPDATE failed! Trying INSERT.");
                    return insertFull(id, jobAcceptedTimestamp, processKey, state, lastEventTimestamp, pausedBucketKey)
                        .map(jobRecord -> 1);
                } else {
                    return Mono.just(1);
                }
            });
    }

    //- SELECT ---------------------------------------------------------------------------------------------------------

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

    private Mono<JobRecord> insertFull(long id, Instant jobAcceptedTimestamp, String processKey, String state,
                                       Instant lastEventTimestamp, String pausedBucketKey) {

        // TODO: processKey ==> processId by DB query or DB view
        final long processId = Long.parseLong(processKey.substring(1), 10);
        final JobRecord jobRecord = new JobRecord(id, jobAcceptedTimestamp, lastEventTimestamp, processId);
        jobRecord.setStatus(state);
        jobRecord.setPausedBucketKey(pausedBucketKey);
        final Instant lastRecordUpdateTimestamp = Instant.now();
        jobRecord.setLastRecordUpdateTimestamp(lastRecordUpdateTimestamp);

        final long latency = lastRecordUpdateTimestamp.toEpochMilli() - lastEventTimestamp.toEpochMilli();
        this.latencyTimer.record(latency, TimeUnit.MILLISECONDS);

        return r2dbcEntityTemplate.insert(jobRecord);
    }

    private Mono<Integer> insertIgnoreConflict(long id, Instant jobAcceptedTimestamp, String processKey, String state,
                                               Instant lastEventTimestamp, String pausedBucketKey) {

        // TODO: processKey ==> processId by DB query or DB view
        final long processId = Long.parseLong(processKey.substring(1), 10);
        final Instant lastRecordUpdateTimestamp = Instant.now();
        final long latency = lastRecordUpdateTimestamp.toEpochMilli() - lastEventTimestamp.toEpochMilli();
        this.latencyTimer.record(latency, TimeUnit.MILLISECONDS);

        return jobRecordRepository.insertIgnoreConflict(id, jobAcceptedTimestamp, lastEventTimestamp,
            lastRecordUpdateTimestamp, state, pausedBucketKey, processId);
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
    protected Mono<Integer> update(boolean checkTimestamp, long id, String state, Instant lastEventTimestamp, String pausedBucketKey) {

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

        final long latency = lastRecordUpdateTimestamp.toEpochMilli() - lastEventTimestamp.toEpochMilli();
        this.latencyTimer.record(latency, TimeUnit.MILLISECONDS);

        return r2dbcEntityTemplate
            .update(JobRecord.class)
            .matching(Query.query(criteria))
            .apply(update)
            .doOnNext(updateCount -> LOGGER.debug("Update id={} state={} lastEventTimestamp={} updated {} rows",
                id, state, lastEventTimestamp, updateCount));
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
            .selectOne(Query.query(where(JobRecord.ATTRIBUTE_id).is(id)), JobRecord.class)
            .doOnNext(jobRecord -> LOGGER.debug("findById id={} state={}", id, jobRecord.getStatus()));
    }

    /**
     * Query JobRecord by its id using "FOR UPDATE"
     *
     * @param id the job record id
     * @return A Mono of the found JobRecord or an empty Mono
     */
    private Mono<JobRecord> findByIdForUpdate(long id) {

        LOGGER.debug("findByIdForUpdate {}", id);
        return jobRecordRepository.findByIdForUpdate(id)
            .doOnNext(jobRecord -> LOGGER.debug("findByIdForUpdate id={} state={}", id, jobRecord.getStatus()));
    }
}
