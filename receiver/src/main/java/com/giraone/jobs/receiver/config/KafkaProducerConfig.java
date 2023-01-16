package com.giraone.jobs.receiver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public SenderOptions<String, String> kafkaSenderOptions(KafkaProperties kafkaProperties) {

        SenderOptions<String, String> basicSenderOptions = SenderOptions
            .create(kafkaProperties.buildProducerProperties());

        // set some properties programmatically
        basicSenderOptions = basicSenderOptions
            .maxInFlight(1) // to keep ordering, prevent duplicate messages (and avoid data loss )
        ;

        return basicSenderOptions;
    }

    @Bean
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate(SenderOptions<String, String> kafkaSenderOptions) {

        return new ReactiveKafkaProducerTemplate<>(kafkaSenderOptions);
    }
}
