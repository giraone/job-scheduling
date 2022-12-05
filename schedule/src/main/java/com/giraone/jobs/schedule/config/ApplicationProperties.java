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

    private boolean showConfigOnStartup;
    private boolean disableStopper;
    private long sleep = 0;
    private RetrySpecification retry = new RetrySpecification();

    private Topics topics;
    private Id id;

    private long pausedDeciderQueryWaitSeconds = 10;

    private String jobAdminHost;
    private String jobAdminPath;
    private int jobAdminBlockSeconds = 30;

    @PostConstruct
    private void startup() {
        if (this.showConfigOnStartup) {
            LOGGER.info(this.toString());
        }
        UtilsAndConstants.sleepTime = this.sleep;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Generated
    public static class Topics {
        private Topic queueAccepted;
        private Topic queueScheduledA01;
        private Topic queueScheduledA02;
        private Topic queueScheduledA03;
        private Topic queuePausedB01;
        private Topic queuePausedB02;
        private Topic queueFailedA01;
        private Topic queueFailedA02;
        private Topic queueFailedA03;
        private Topic queueCompleted;
        private Topic queueNotified;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Generated
    public static class Topic {
        private String topic;
        private String error;
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
        private String processAgentA01;
        private String processAgentA02;
        private String processAgentA03;
        private String processResumeB01;
        private String processResumeB02;
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
}
