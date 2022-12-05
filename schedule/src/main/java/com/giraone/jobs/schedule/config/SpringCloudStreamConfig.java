package com.giraone.jobs.schedule.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.common.ObjectMapperBuilder;
import com.giraone.jobs.schedule.exceptions.CustomProductionExceptionHandler;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Properties;

import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;

@Configuration
public class SpringCloudStreamConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudStreamConfig.class);

    @Bean
    StreamsBuilderFactoryBeanConfigurer streamsCustomizer() {

        LOGGER.info("Performing StreamsBuilderFactoryBeanConfigurer setup using EventProcessor.streamsCustomizer");
        return new StreamsBuilderFactoryBeanConfigurer() {

            @Override
            public void configure(StreamsBuilderFactoryBean factoryBean) {

                Properties properties = factoryBean.getStreamsConfiguration();
                if (properties != null) {
                    LOGGER.info("Initializing CustomProductionExceptionHandler using EventProcessor.streamsCustomizer");
                    properties.put(
                        StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG,
                        CustomProductionExceptionHandler.class);
                } else {
                    LOGGER.error("Cannot set CustomProductionExceptionHandler! Properties are null.");
                }

                LOGGER.info("Initializing UncaughtExceptionHandler using EventProcessor.streamsCustomizer");
                factoryBean.setKafkaStreamsCustomizer(kafkaStreams -> kafkaStreams.setUncaughtExceptionHandler(
                    exception -> {
                        LOGGER.error(">>> UNCAUGHT EXCEPTION", exception);
                        // See https://cwiki.apache.org/confluence/display/KAFKA/KIP-671%3A+Introduce+Kafka+Streams+Specific+Uncaught+Exception+Handler
                        // The current thread is shutdown and transits to state DEAD.
                        // A new thread is started if the Kafka Streams client is in state RUNNING or REBALANCING.
                        return REPLACE_THREAD;
                    }));
            }

            @Override
            public int getOrder() {
                return Integer.MAX_VALUE;
            }
        };
    }

    @Bean
    public Serde<JobScheduledEvent> forceMessage2Serde() {
        final ObjectMapper mapper = ObjectMapperBuilder.build(false, false);
        return new JsonSerde<>(JobScheduledEvent.class, mapper);
    }
}
