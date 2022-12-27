package com.giraone.jobs.materialize.persistence;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface JobRecordRepository extends R2dbcRepository<JobRecord, String> {

    @Query("SELECT * FROM job_record WHERE id = :id FOR UPDATE")
    Mono<JobRecord> findByIdForUpdate(long id);

    @Modifying
    @Query("""
        INSERT INTO job_record
        VALUES(:id, :jobAcceptedTimestamp, :lastEventTimestamp, :lastRecordUpdateTimestamp, :status, :pausedBucketKey, :processId)
        """)
    Mono<Integer> insert(
        long id, Instant jobAcceptedTimestamp, Instant lastEventTimestamp,
        Instant lastRecordUpdateTimestamp, String status, String pausedBucketKey, long processId);

    @Modifying
    @Query("""
        INSERT INTO job_record
        VALUES(:id, :jobAcceptedTimestamp, :lastEventTimestamp, :lastRecordUpdateTimestamp, :status, :pausedBucketKey, :processId)
        ON CONFLICT (id) DO NOTHING
        """)
    Mono<Integer> insertIgnoreConflict(
        long id, Instant jobAcceptedTimestamp, Instant lastEventTimestamp,
        Instant lastRecordUpdateTimestamp, String status, String pausedBucketKey, long processId);

    @Modifying
    @Query("""
        INSERT INTO job_record
        VALUES(:id, :jobAcceptedTimestamp, :lastEventTimestamp, :lastRecordUpdateTimestamp, :status, :pausedBucketKey, :processId)
        ON CONFLICT (id) DO UPDATE SET
        last_event_timestamp=:lastEventTimestamp, last_record_update_timestamp=:lastRecordUpdateTimestamp, status=:status, paused_bucket_key=:pausedBucketKey
        """)
    Mono<Integer> insertOnConflictUpdate(
        long id, Instant jobAcceptedTimestamp, Instant lastEventTimestamp,
        Instant lastRecordUpdateTimestamp, String status, String pausedBucketKey, long processId);
}
