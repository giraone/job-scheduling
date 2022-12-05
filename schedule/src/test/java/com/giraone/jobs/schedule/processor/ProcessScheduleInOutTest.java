package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.config.TestConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// See https://blog.mimacom.com/testing-apache-kafka-with-spring-boot-junit5/

@EmbeddedKafka(
    controlledShutdown = true,
    topics = {
        TestConfig.TOPIC_accepted,
        TestConfig.TOPIC_accepted_ERR,
        TestConfig.TOPIC_scheduled,
        TestConfig.TOPIC_paused
    },
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
    partitions = 3
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@ActiveProfiles({"test", "processSchedule"})
// Thread.sleep
@SuppressWarnings("java:S2925")
class ProcessScheduleInOutTest extends AbstractInOutTest {

    @MockBean
    PausedDecider pausedDecider;

    @BeforeAll
    public void setup() {
        super.setup();
        embeddedKafka.consumeFromEmbeddedTopics(consumer, TestConfig.TOPIC_scheduled, TestConfig.TOPIC_paused, TestConfig.TOPIC_accepted_ERR);
    }

    @ParameterizedTest
    @CsvSource({
        "false",
        "true"
    })
    void testProcessWorks(boolean paused) throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testProcessWorks START", this.getClass().getName());
        DefaultKafkaProducerFactory<String, String> pf = buildDefaultKafkaProducerFactory();

        when(pausedDecider.isProcessPaused(anyString())).thenReturn(paused ? 1 : 0);

        try {
            KafkaTemplate<String, String> template = new KafkaTemplate<>(pf, true);
            JobAcceptedEvent jobAcceptedEvent = new JobAcceptedEvent(12L, "A01", Instant.now(), "");
            template.send(TestConfig.TOPIC_accepted, jobAcceptedEvent.getMessageKey(), objectMapper.writeValueAsString(jobAcceptedEvent));
            Thread.sleep(TestConfig.DEFAULT_SLEEP_AFTER_PRODUCE_TIME);

            String topicName = paused ? TestConfig.TOPIC_paused : TestConfig.TOPIC_scheduled;
            ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(
                consumer, topicName, TestConfig.DEFAULT_CONSUMER_POLL_TIME);
            LOGGER.info("{}} testProcessWorks POLL TOPIC {} RETURNED key={} value={}",
                getClass().getName(), topicName, consumerRecord.key(), consumerRecord.value());
            assertThat(consumerRecord.key()).isNotNull();
            assertThat(consumerRecord.value()).isNotNull();
            assertThat(consumerRecord.value()).contains("\"id\":12");
            assertThat(consumerRecord.value()).contains("\"processKey\":1");

            if (paused) {
                JobPausedEvent jobPausedEvent = objectMapper.readValue(consumerRecord.value(), JobPausedEvent.class);
                assertThat(jobPausedEvent.getMessageKey()).isNotNull();
            } else {
                JobScheduledEvent jobScheduledEvent = objectMapper.readValue(consumerRecord.value(), JobScheduledEvent.class);
                assertThat(jobScheduledEvent.getMessageKey()).isNotNull();
            }

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
            template.send(TestConfig.TOPIC_accepted, "any-key", "{}");
            Thread.sleep(TestConfig.DEFAULT_SLEEP_AFTER_PRODUCE_TIME);
            ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(
                consumer, TestConfig.TOPIC_accepted_ERR, TestConfig.DEFAULT_CONSUMER_POLL_TIME);

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
