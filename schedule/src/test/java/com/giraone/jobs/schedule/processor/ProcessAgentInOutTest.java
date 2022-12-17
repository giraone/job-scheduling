package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
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
        TOPIC_scheduled_A02,
        TOPIC_scheduled_ERR,
        TOPIC_completed
    },
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@ActiveProfiles({"test", "processAgentA01"})
// Thread.sleep
@SuppressWarnings("java:S2925")
class ProcessAgentInOutTest extends AbstractInOutTest {

    @BeforeAll
    public void setup() {
        super.setup();
        embeddedKafka.consumeFromEmbeddedTopics(consumer, TOPIC_scheduled_ERR, TOPIC_completed);
    }

    @Test
    void testProcessWorks() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testProcessWorks START", getClass().getName());

        // act
        String id = TsidCreator.getTsid256().toString();
        JobScheduledEvent jobScheduledEvent = new JobScheduledEvent("12", "A01", Instant.now(), "");
        produce(jobScheduledEvent, TOPIC_scheduled_A01);

        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_completed);
        assertThat(consumerRecord.key()).isNotNull();
        assertThat(consumerRecord.value()).isNotNull();
        assertThat(consumerRecord.value()).contains("\"id\":\"" + id + "\"");
        assertThat(consumerRecord.value()).contains("\"processKey\":\"A01\"");

        JobCompletedEvent JobCompletedEvent = objectMapper.readValue(consumerRecord.value(), JobCompletedEvent.class);
        assertThat(JobCompletedEvent.getMessageKey()).isNotNull();
        assertThat(JobCompletedEvent.getPayload()).startsWith("https://link/00000012");
    }

    @Test
    void testDeserializeException() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testDeserializeException START", getClass().getName());
        // act
        produce("x", TOPIC_scheduled_A01);
        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_scheduled_ERR);
        assertThat(consumerRecord.value()).contains("Exception");
    }

    @Test
    void testRuntimeException() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testRuntimeException START", getClass().getName());
        // act
        JobScheduledEvent event = new JobScheduledEvent(); // id is null
        produce(event, TOPIC_scheduled_A01);
        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_scheduled_ERR);
        assertThat(consumerRecord.value()).contains("Exception");
    }
}
