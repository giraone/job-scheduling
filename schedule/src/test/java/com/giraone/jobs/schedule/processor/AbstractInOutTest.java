package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.giraone.jobs.schedule.config.TestConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Needed, because otherwise setup must be static
public abstract class AbstractInOutTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractInOutTest.class);

    protected static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected EmbeddedKafkaBroker embeddedKafka;

    protected Consumer<String, String> consumer;

    protected void setup() {
        LOGGER.info("TEST AbstractInOutTest setup {}", embeddedKafka.getBrokerAddresses()[0]);
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
        LOGGER.info("TEST AbstractInOutTest tearDown");
        consumer.close();
        // Do not use embeddedKafka.destroy(); Use @DirtiesContext on the concrete class.
        System.clearProperty("spring.kafka.bootstrap-servers");
        System.clearProperty("spring.cloud.stream.kafka.streams.binder.brokers");
    }

    //- CONSUME --------------------------------------------------------------------------------------------------------

    protected ConsumerRecord<String, String> pollTopic(String topic) {
        LOGGER.info("POLLING TOPIC \"{}\"", topic);
        ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(
            consumer, topic, DEFAULT_CONSUMER_POLL_TIME.toMillis());
        LOGGER.info("POLL TOPIC \"{}\" RETURNED key={} value={}",
            topic, consumerRecord.key(), consumerRecord.value());
        return consumerRecord;
    }

    protected void pollTopicForBeingEmpty(String topic) {
        LOGGER.info("POLLING TOPIC \"{}\" TO BE EMPTY", topic);
        assertThatThrownBy(() -> KafkaTestUtils.getSingleRecord(consumer, topic, DEFAULT_CONSUMER_POLL_TIME.toMillis()))
            .hasMessageContaining("No records found for topic");
    }

    //- PRODUCE --------------------------------------------------------------------------------------------------------

    protected DefaultKafkaProducerFactory<String, String> buildDefaultKafkaProducerFactory() {
        final Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        // For testing, we use StringSerializer for message key and message body
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProps);
    }

    protected void produce(Object messageObject, String topic) throws JsonProcessingException, InterruptedException {

        DefaultKafkaProducerFactory<String, String> pf = buildDefaultKafkaProducerFactory();
        KafkaTemplate<String, String> template = new KafkaTemplate<>(pf, true);

        String messageKey = String.format("%08d", System.currentTimeMillis()); // to test StringSerializer
        LOGGER.info("SENDING TO TOPIC \"{}\"", topic);
        String messageBody = objectMapper.writeValueAsString(messageObject);
        template.send(topic, messageKey, messageBody);
        LOGGER.info("SENT TO TOPIC \"{}\"", topic);
        Thread.sleep(DEFAULT_SLEEP_AFTER_PRODUCE_TIME.toMillis());
        LOGGER.info("SLEPT {} AFTER SENT TO TOPIC \"{}\"", DEFAULT_SLEEP_AFTER_PRODUCE_TIME, topic);
    }

    protected void produceAndCheckEmpty(Object message, String topicIn, String topicOut) throws JsonProcessingException, InterruptedException {
        produce(message, topicIn);
        assertThatThrownBy(() -> KafkaTestUtils.getSingleRecord(consumer, topicOut, DEFAULT_CONSUMER_POLL_TIME.toMillis()))
            .hasMessageContaining("No records found for topic");
    }

    protected ConsumerRecord<String, String> produceAndAwaitConsume(Object message, String topicIn, String topicOut)
        throws JsonProcessingException, InterruptedException {

        produce(message, topicIn);
        ConsumerRecord<String, String> consumerRecord = pollTopic(topicOut);
        assertThat(consumerRecord.key()).isNotNull();
        assertThat(consumerRecord.value()).isNotNull();
        return consumerRecord;
    }

    //- THREADS --------------------------------------------------------------------------------------------------------

    protected boolean awaitOnNewThread(Callable callable) {
        return awaitOnNewThread(callable, DEFAULT_THREAD_WAIT_TIME);
    }

    protected Boolean awaitOnNewThread(Callable<Boolean> callable, Duration waitAtMost) {

        final AtomicBoolean result = new AtomicBoolean();
        final Runnable runnable = () -> {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        synchronized (thread) {
            try {
                thread.wait(waitAtMost.toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result.get();
    }
}
