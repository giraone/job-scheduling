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
    private static final String DEFAULT_PROCESSOR_NAMES = "processSchedule,processResumeB01,processResumeB02,processAgentA01,processAgentA02,processAgentA03,processNotify";

    private boolean showConfigOnStartup;
    private boolean disableStopper;
    private long sleep = 0;
    private RetrySpecification retry = new RetrySpecification();

    private Topics topics;
    private Id id;

    private LoadProcessStatus loadProcessStatus;
    private String jobAdminScheme = "http";
    private String jobAdminHost;
    private String jobAdminPathAll;
    private int jobAdminBlockMaxSeconds = 30;

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
            topics.topicJobAcceptedErr,
            topics.getTopicJobScheduled("A01"),
            topics.getTopicJobScheduled("A02"),
            topics.getTopicJobScheduled("A03"),
            topics.topicJobScheduledErr,
            topics.getTopicJobPaused("B01"),
            topics.getTopicJobPaused("B02"),
            topics.topicJobPausedErr,
            topics.getTopicJobFailed("A01"),
            topics.getTopicJobFailed("A02"),
            topics.getTopicJobFailed("A03"),
            topics.topicJobFailedErr,
            topics.topicJobCompleted,
            topics.topicJobCompletedErr,
            topics.topicJobNotified,
            topics.topicJobDelivered,
        };
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Generated
    public static class Topics {
        private String topicJobAccepted;
        private String topicJobAcceptedErr;
        private String topicJobScheduled;
        private String topicJobScheduledErr;
        private String topicJobPaused;
        private String topicJobPausedErr;
        private String topicJobFailed;
        private String topicJobFailedErr;
        private String topicJobCompleted;
        private String topicJobCompletedErr;
        private String topicJobNotified;
        private String topicJobDelivered;

        public String getTopicJobScheduled(String agentKey) {
            return topicJobScheduled + "-" + agentKey;
        }
        public String getTopicJobFailed(String agentKey) {
            return topicJobFailed + "-" +  agentKey;
        }

        public String getTopicJobPaused(String pausedBucketKey) {
            return topicJobPaused + "-" + pausedBucketKey;
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
         * Milliseconds to wait between each reload of process status (paused, active) from jobadmin service
         */
        private long fixedRateMs;
        /**
         * Initial wait time after start to load process status (paused, active) from jobadmin service
         */
        private long initialDelayMs;
    }
}
