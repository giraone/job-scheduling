package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Needed, because otherwise setup must be static
public abstract class AbstractInOutTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractInOutTest.class);

    protected static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected EmbeddedKafkaBroker embeddedKafka;

    protected Consumer<String, String> consumer;

    protected void setup() {
        LOGGER.info("TEST EventProcessorTestUsingEmbeddedKafka setup {}", embeddedKafka.getBrokerAddresses()[0]);
        System.setProperty("spring.kafka.bootstrap-servers", embeddedKafka.getBrokersAsString());
        System.setProperty("spring.cloud.stream.kafka.streams.binder.brokers", embeddedKafka.getBrokersAsString());

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("group", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
            consumerProps, new StringDeserializer(), new StringDeserializer());
        consumer = consumerFactory.createConsumer();
    }

    @AfterAll
    public void tearDown() {
        LOGGER.info("TEST EventProcessorTestUsingEmbeddedKafka tearDown");
        consumer.close();
        // Do not use embeddedKafka.destroy(); Use @DirtiesContext on the concrete class.
        System.clearProperty("spring.kafka.bootstrap-servers");
        System.clearProperty("spring.cloud.stream.kafka.streams.binder.brokers");
    }

    protected DefaultKafkaProducerFactory<String, String> buildDefaultKafkaProducerFactory() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        // Needed for sendBridge, when Message is used to set a string key
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProps);
    }
}
