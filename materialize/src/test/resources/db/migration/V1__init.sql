CREATE TABLE job_record (
 id BIGINT NOT NULL,
 job_accepted_timestamp TIMESTAMP,
 last_event_timestamp TIMESTAMP,
 last_record_update_timestamp TIMESTAMP,
 status VARCHAR(255),
 paused_bucket_key VARCHAR(255),
 process_id BIGINT,
 CONSTRAINT pk_job_record PRIMARY KEY (id)
);
