package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobNotifiedEvent;
import com.giraone.jobs.schedule.config.TestConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// See https://blog.mimacom.com/testing-apache-kafka-with-spring-boot-junit5/

@EmbeddedKafka(
    controlledShutdown = true,
    topics = {
        TestConfig.TOPIC_completed,
        TestConfig.TOPIC_notified,
        TestConfig.TOPIC_completed_ERR
    },
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
    partitions = 3
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@ActiveProfiles({"test", "processNotify"})
// Thread.sleep
@SuppressWarnings("java:S2925")
class ProcessNotifyInOutTest extends AbstractInOutTest {

    @BeforeAll
    public void setup() {
        super.setup();
        embeddedKafka.consumeFromEmbeddedTopics(consumer, TestConfig.TOPIC_notified, TestConfig.TOPIC_completed_ERR);
    }

    @Test
    void testProcessWorks() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testProcessWorks START", getClass().getName());
        DefaultKafkaProducerFactory<String, String> pf = buildDefaultKafkaProducerFactory();

        try {
            KafkaTemplate<String, String> template = new KafkaTemplate<>(pf, true);
            JobCompletedEvent JobCompletedEvent = new JobCompletedEvent(12L, "A01", Instant.now(), "link");
            template.send(TestConfig.TOPIC_completed, JobCompletedEvent.getMessageKey(), objectMapper.writeValueAsString(JobCompletedEvent));
            Thread.sleep(TestConfig.DEFAULT_SLEEP_AFTER_PRODUCE_TIME);

            ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(
                consumer, TestConfig.TOPIC_notified, TestConfig.DEFAULT_CONSUMER_POLL_TIME);
            LOGGER.info("{}} testProcessWorks POLL TOPIC RETURNED key={} value={}",
                getClass().getName(), consumerRecord.key(), consumerRecord.value());
            assertThat(consumerRecord.key()).isNotNull();
            assertThat(consumerRecord.value()).isNotNull();
            assertThat(consumerRecord.value()).contains("\"id\":12");
            assertThat(consumerRecord.value()).contains("\"processKey\":\"A01\"");

            JobNotifiedEvent JobNotifiedEvent = objectMapper.readValue(consumerRecord.value(), JobNotifiedEvent.class);
            assertThat(JobNotifiedEvent.getMessageKey()).isNotNull();
        } finally {
            pf.destroy();
        }
    }

    @Test
    void testRuntimeException() throws InterruptedException {

        LOGGER.info("{} testRuntimeException START", getClass().getName());
        DefaultKafkaProducerFactory<String, String> pf = buildDefaultKafkaProducerFactory();

        try {
            KafkaTemplate<String, String> template = new KafkaTemplate<>(pf, true);
            template.send(TestConfig.TOPIC_completed, "req-id-runtime-err", "{}");
            Thread.sleep(TestConfig.DEFAULT_SLEEP_AFTER_PRODUCE_TIME);
            ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(
                consumer, TestConfig.TOPIC_completed_ERR, TestConfig.DEFAULT_CONSUMER_POLL_TIME);

            assertThat(consumerRecord).isNotNull();
            LOGGER.info("{} testRuntimeException POLL TOPIC_ERR RETURNED key={} value={}",
                getClass().getName(), consumerRecord.key(), consumerRecord.value());
            assertThat(consumerRecord.key()).isNotNull();
            assertThat(consumerRecord.value()).contains("Exception");
        } finally {
            pf.destroy();
        }
    }
}
