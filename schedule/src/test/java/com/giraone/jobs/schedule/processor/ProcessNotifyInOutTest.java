package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobNotifiedEvent;
import com.github.f4b6a3.tsid.TsidCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static com.giraone.jobs.schedule.config.TestConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

// See https://blog.mimacom.com/testing-apache-kafka-with-spring-boot-junit5/
@EmbeddedKafka(
    controlledShutdown = true,
    topics = {
        TOPIC_completed,
        TOPIC_completed_ERR,
        TOPIC_notified
    },
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
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
        embeddedKafka.consumeFromEmbeddedTopics(consumer, TOPIC_completed_ERR, TOPIC_notified);
    }

    @Test
    void testProcessWorks() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testProcessWorks START", getClass().getName());

        // act
        String id = TsidCreator.getTsid256().toString();
        JobCompletedEvent jobCompletedEvent = new JobCompletedEvent(id, "V001", Instant.now(), "link-to-result", "A01");
        produce(jobCompletedEvent, TOPIC_completed);

        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_notified);
        assertThat(consumerRecord.key()).isNotNull();
        assertThat(consumerRecord.value()).isNotNull();
        assertThat(consumerRecord.value()).contains("\"id\":\"" + id + "\"");
        assertThat(consumerRecord.value()).contains("\"processKey\":\"V001\"");
        assertThat(consumerRecord.value()).contains("\"agentKey\":\"A01\"");
        JobNotifiedEvent JobNotifiedEvent = objectMapper.readValue(consumerRecord.value(), JobNotifiedEvent.class);
        assertThat(JobNotifiedEvent.getMessageKey()).isNotNull();
    }

    @Test
    void testDeserializeException() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testDeserializeException START", getClass().getName());
        // act
        produce("x", TOPIC_completed);
        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_completed_ERR);
        assertThat(consumerRecord.value()).contains("Exception");
    }

    @Test
    void testRuntimeException() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testRuntimeException START", getClass().getName());
        // act
        JobCompletedEvent event = new JobCompletedEvent(); // id is null
        produce(event, TOPIC_completed);
        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_completed_ERR);
        assertThat(consumerRecord.value()).contains("Exception");
    }
}
