package com.giraone.jobs.schedule.config;

import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Properties specific to application.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
@With
@ToString
// exclude from test coverage
@Generated
public class ApplicationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProperties.class);

    private static final int DEFAULT_RETRY_FIXED_DELAY_IN_MILLISECONDS = 500;
    private static final int RETRY_DEFAULT_NUMBER_OF_ATTEMPTS = 2;
    private static final String DEFAULT_PROCESSOR_NAMES = "processSchedule,processResumeB01,processResumeB02,processAgentA01,processAgentA02,processAgentA02,processNotify";

    private boolean showConfigOnStartup;
    private boolean disableStopper;
    private long sleep = 0;
    private RetrySpecification retry = new RetrySpecification();

    private Topics topics;
    private Id id;

    private LoadProcessStatus loadProcessStatus;
    private String jobAdminHost;
    private String jobAdminPathAll;
    private int jobAdminBlockSeconds = 30;

    /**
     * Comma separated names of processors.
     */
    private String processorNames = DEFAULT_PROCESSOR_NAMES;
    /**
     * Comma separated names of topics.
     */
    private String[] outboundTopicList = new String[0];

    @PostConstruct
    private void startup() {
        if (showConfigOnStartup) {
            LOGGER.info(this.toString());
        }
        UtilsAndConstants.sleepTime = sleep;
        outboundTopicList = new String[] {
            topics.queueAcceptedErr,
            topics.getQueueScheduled("A01"),
            topics.getQueueScheduled("A02"),
            topics.getQueueScheduled("A03"),
            topics.queueScheduledErr,
            topics.getQueuePaused("B01"),
            topics.getQueuePaused("B02"),
            topics.queuePausedErr,
            topics.getQueueFailed("A01"),
            topics.getQueueFailed("A02"),
            topics.getQueueFailed("A03"),
            topics.queueFailedErr,
            topics.queueCompleted,
            topics.queueCompletedErr,
            topics.queueNotified,
            topics.queueDelivered,
        };
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Generated
    public static class Topics {
        private String queueAccepted;
        private String queueAcceptedErr;
        private String queueScheduled;
        private String queueScheduledErr;
        private String queuePaused;
        private String queuePausedErr;
        private String queueFailed;
        private String queueFailedErr;
        private String queueCompleted;
        private String queueCompletedErr;
        private String queueNotified;
        private String queueDelivered;

        public String getQueueScheduled(String agentKey) {
            return queueScheduled + "-" + agentKey;
        }
        public String getQueueFailed(String agentKey) {
            return queueFailed + "-" +  agentKey;
        }

        public String getQueuePaused(String pausedBucketKey) {
            return queuePaused + "-" + pausedBucketKey;
        }
    }

    /**
     * Application ids for Kafka Streams
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Generated
    public static class Id {
        private String processSchedule;
        private String processAgent;
        private String processResume;
        private String processNotify;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static final class RetrySpecification {
        /**
         * Set a fix delay between each retry (no exponential backoff)
         */
        private int fixedDelayMillis = DEFAULT_RETRY_FIXED_DELAY_IN_MILLISECONDS;
        /**
         * 0 means no retry, 1 means once retry
         */
        private int attempts = RETRY_DEFAULT_NUMBER_OF_ATTEMPTS;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static final class LoadProcessStatus {
        /**
         * Milliseconds to wait between each reload of process status (paused, active) from job-admin service
         */
        private long fixedRateMs;
        /**
         * Initial wait time after start to load process status (paused, active) from job-admin service
         */
        private long initialDelayMs;
    }
}
