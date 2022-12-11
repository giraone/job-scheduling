package com.giraone.jobs.schedule.config;

import java.time.Duration;

public class TestConfig {

    public static final String TOPIC_accepted = "job-accepted";
    public static final String TOPIC_accepted_ERR = "job-accepted-err";

    public static final String TOPIC_scheduled_A01 = "job-scheduled-A01";
    public static final String TOPIC_scheduled_A02 = "job-scheduled-A02";
    public static final String TOPIC_scheduled_A03 = "job-scheduled-A03";
    public static final String TOPIC_scheduled_ERR = "job-scheduled-err";

    public static final String TOPIC_paused_B01 = "job-paused-B01";
    public static final String TOPIC_paused_B02 = "job-paused-B02";
    public static final String TOPIC_paused_ERR = "job-paused-err";

    public static final String TOPIC_completed = "job-completed";
    public static final String TOPIC_completed_ERR = "job-completed-err";

    public static final String TOPIC_failed = "job-failed";
    public static final String TOPIC_failed_ERR = "job-failed-err";

    public static final String TOPIC_notified = "job-notified";

    public static final Duration DEFAULT_SLEEP_AFTER_PRODUCE_TIME = Duration.ofSeconds(1);
    public static final Duration DEFAULT_CONSUMER_POLL_TIME = Duration.ofSeconds(1);
    public static final Duration DEFAULT_THREAD_WAIT_TIME = Duration.ofSeconds(1);
}
