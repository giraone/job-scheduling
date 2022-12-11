package com.giraone.jobs.receiver.service;

import com.giraone.jobs.receiver.config.ApplicationProperties;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerService.class);

    private final String topic;
    private final ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate;

    private final AtomicLong counterSent = new AtomicLong();
    private final AtomicLong counterFailed = new AtomicLong();

    public ProducerService(ApplicationProperties applicationProperties,
                           ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate) {

        this.topic = applicationProperties.getJobAcceptedTopic();
        this.reactiveKafkaProducerTemplate = reactiveKafkaProducerTemplate;
    }

    public Mono<Map<String, Object>> getMetrics() {

        return Mono.just(Map.of("sent", counterSent, "failed", counterFailed));
    }

    public Mono<String> send(String event) {

        final Tsid tsid = TsidCreator.getTsid256();
        final String messageKey = tsid.toString();
        return reactiveKafkaProducerTemplate
            .send(topic, messageKey, event)
            .doOnError(e -> {
                LOGGER.error("Send to topic \"{}\" failed.", topic, e);
                counterFailed.incrementAndGet();
            })
            .doOnNext(r -> {
                LOGGER.info("Send to topic \"{}\" successful.", topic);
                counterSent.incrementAndGet();
            })
            .map(r -> messageKey);
    }
}
