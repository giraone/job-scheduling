package com.giraone.jobs.schedule.config;

public class TestConfig {

    public static final String TOPIC_accepted = "accepted";
    public static final String TOPIC_accepted_ERR = "accepted-err";

    public static final String TOPIC_scheduled = "scheduled-A01";
    public static final String TOPIC_scheduled_ERR = "scheduled-err";

    public static final String TOPIC_paused = "paused-B01";
    public static final String TOPIC_paused_ERR = "paused-err";

    public static final String TOPIC_completed = "completed";
    public static final String TOPIC_completed_ERR = "completed-err";

    public static final String TOPIC_failed = "failed";
    public static final String TOPIC_failed_ERR = "failed-err";

    public static final String TOPIC_notified = "notified";

    public static final long DEFAULT_SLEEP_AFTER_PRODUCE_TIME = 1000L;
    public static final long DEFAULT_CONSUMER_POLL_TIME = 3000L;
}
