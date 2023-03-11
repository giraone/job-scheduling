package com.giraone.jobs.receiver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import com.giraone.jobs.receiver.config.ApplicationProperties;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerService.class);

    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

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

    public Mono<String> send(Map<String, Object> event) {

        final Tsid tsid = TsidCreator.getTsid256();
        final String id = tsid.toString();
        event.put("id", id);
        event.put("jobAcceptedTimestamp", Instant.now());
        final String messageBody;
        try {
            messageBody = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return reactiveKafkaProducerTemplate
            .send(topic, id, messageBody)
            .doOnError(e -> {
                LOGGER.error("Send to topic \"{}\" failed.", topic, e);
                counterFailed.incrementAndGet();
            })
            .doOnNext(r -> {
                LOGGER.debug("Send to topic \"{}\", partition={}, offset={} successful.",
                    r.recordMetadata().topic(), r.recordMetadata().partition(), r.recordMetadata().offset());
                counterSent.incrementAndGet();
            })
            .map(r -> id);
    }
}
