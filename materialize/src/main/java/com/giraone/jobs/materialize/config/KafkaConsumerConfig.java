package com.giraone.jobs.materialize.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private final String topicInsert;
    private final List<String> topicsUpdate;

    public KafkaConsumerConfig(ApplicationProperties applicationProperties) {
        topicInsert = applicationProperties.getTopicInsert();
        topicsUpdate = List.of(applicationProperties.getTopicsUpdate().split(","));
        LOGGER.info("Topics are: insert={}, updates={}", topicInsert, topicsUpdate);
    }

    @Bean
    public ReceiverOptions<String, String> kafkaReceiverOptions(KafkaProperties kafkaProperties) {

        ReceiverOptions<String, String> basicReceiverOptions = ReceiverOptions
            .create(kafkaProperties.buildConsumerProperties());

        // See https://projectreactor.io/docs/kafka/release/reference/#kafka-source
        basicReceiverOptions
            .commitInterval(Duration.ZERO) // Disable periodic commits
            .commitBatchSize(0); // Disable commits by batch size

        final List<String> allTopics = new ArrayList<>();
        allTopics.add(topicInsert);
        allTopics.addAll(topicsUpdate);
        ReceiverOptions<String, String> ret = basicReceiverOptions.subscription(allTopics);
        LOGGER.info("ReceiverOptions defined by bean of {} with topics {}", this.getClass().getSimpleName(), allTopics);
        return ret;
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, String> reactiveKafkaConsumerTemplate(ReceiverOptions<String, String> kafkaReceiverOptions) {

        LOGGER.info("Subscription with group id \"{}\" to {}", kafkaReceiverOptions.groupId(), kafkaReceiverOptions.subscriptionTopics());
        return new ReactiveKafkaConsumerTemplate<>(kafkaReceiverOptions);
    }
}