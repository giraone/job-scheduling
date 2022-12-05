package com.giraone.jobs.schedule.clients;

import com.giraone.jobs.schedule.config.ApplicationProperties;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

public abstract class AbstractClient {

    protected final ApplicationProperties applicationProperties;
    protected final Retry retry;

    protected AbstractClient(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        final ApplicationProperties.RetrySpecification retrySpecification = applicationProperties.getRetry();
        this.retry = Retry
            .fixedDelay(retrySpecification.getAttempts(), Duration.ofMillis(retrySpecification.getFixedDelayMillis()))
            .filter(AbstractClient::isRetryableError);
    }

    public Retry getRetry() {
        return retry;
    }

    protected static boolean isRetryableError(Throwable throwable) {
        return (throwable instanceof WebClientResponseException) &&
            ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
    }
}
