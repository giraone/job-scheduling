package com.giraone.jobs.receiver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.core.publisher.Hooks;

import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
// exclude from test coverage
public class ApplicationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProperties.class);
    public static final String DEFAULT_JOB_ACCEPTED_TOPIC = "job-accepted";

    /**
     * Log the configuration to the log on startup
     */
    private boolean showConfigOnStartup = true;
    /**
     * WebFlux Hooks.onOperatorDebug() to get full stack traces. Should not be used in production.
     */
    private boolean debugHooks;
    /**
     * Enable reactor-tools ReactorDebugAgent to get stack traces. Can be used also in production.
     */
    private boolean debugAgent;
    /**
     * Name of event topic with new jobs/entries.
     */
    private String jobAcceptedTopic = DEFAULT_JOB_ACCEPTED_TOPIC;

    @SuppressWarnings("squid:S2629") // invoke conditionally
    @PostConstruct
    private void startup() {
        if (this.showConfigOnStartup) {
            LOGGER.info(this.toString());
        }
        if (this.debugHooks) {
            LOGGER.warn("WEBFLUX DEBUG: Enabling Hooks.onOperatorDebug. DO NOT USE IN PRODUCTION!");
            Hooks.onOperatorDebug();
            if (this.debugAgent) {
                LOGGER.error("WEBFLUX DEBUG: DO NOT USE debug-hooks together with debug-agent!");
            }
        } else if (this.debugAgent) {
            long s = System.currentTimeMillis();
            LOGGER.info("WEBFLUX DEBUG: Enabling ReactorDebugAgent. Init may take 20 seconds! May slow down runtime performance (only) slightly.");
            // See: https://github.com/reactor/reactor-tools and https://github.com/reactor/reactor-core/tree/main/reactor-tools
            //            ReactorDebugAgent.init();
            //            ReactorDebugAgent.processExistingClasses();
            LOGGER.info("WEBFLUX DEBUG: ReactorDebugAgent.processExistingClasses finished in {} ms", System.currentTimeMillis() - s);
        }
    }

    public boolean isShowConfigOnStartup() {
        return showConfigOnStartup;
    }

    public void setShowConfigOnStartup(boolean showConfigOnStartup) {
        this.showConfigOnStartup = showConfigOnStartup;
    }

    public boolean isDebugHooks() {
        return debugHooks;
    }

    public void setDebugHooks(boolean debugHooks) {
        this.debugHooks = debugHooks;
    }

    public boolean isDebugAgent() {
        return debugAgent;
    }

    public void setDebugAgent(boolean debugAgent) {
        this.debugAgent = debugAgent;
    }

    public String getJobAcceptedTopic() {
        return jobAcceptedTopic;
    }

    public void setJobAcceptedTopic(String jobAcceptedTopic) {
        this.jobAcceptedTopic = jobAcceptedTopic;
    }

    @Override
    public String toString() {
        return "ApplicationProperties{" +
            "showConfigOnStartup=" + showConfigOnStartup +
            ", debugHooks=" + debugHooks +
            ", debugAgent=" + debugAgent +
            ", jobAcceptedTopic='" + jobAcceptedTopic + '\'' +
            '}';
    }
}
