package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.service.PausedDecider;
import com.github.f4b6a3.tsid.TsidCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static com.giraone.jobs.schedule.config.TestConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// See https://blog.mimacom.com/testing-apache-kafka-with-spring-boot-junit5/
@EmbeddedKafka(
    controlledShutdown = true,
    topics = {
        TOPIC_accepted,
        TOPIC_accepted_ERR,
        TOPIC_scheduled_A01,
        TOPIC_paused_B01
    },
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
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
        embeddedKafka.consumeFromEmbeddedTopics(consumer, TOPIC_accepted_ERR, TOPIC_scheduled_A01, TOPIC_paused_B01);
    }

    @ParameterizedTest
    @CsvSource({
        "false",
        "true"
    })
    void testProcessWorks(boolean paused) throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testProcessWorks START", this.getClass().getName());

        // arrange
        when(pausedDecider.getBucketIfProcessPaused(anyString())).thenReturn(paused ? "B01" : null);

        // act
        String id = TsidCreator.getTsid256().toString();
        JobAcceptedEvent jobAcceptedEvent = new JobAcceptedEvent(id, "A01", Instant.now(), "");
        produce(jobAcceptedEvent, TOPIC_accepted);

        // assert
        String topicNameSent = paused ? TOPIC_paused_B01 : TOPIC_scheduled_A01;
        String topicNameNotSent = !paused ? TOPIC_paused_B01 : TOPIC_scheduled_A01;

        pollTopicForBeingEmpty(topicNameNotSent);

        ConsumerRecord<String, String> consumerRecord = pollTopic(topicNameSent);
        assertThat(consumerRecord.key()).isNotNull();
        assertThat(consumerRecord.value()).isNotNull();
        assertThat(consumerRecord.value()).contains("\"id\":\"" + id + "\"");
        assertThat(consumerRecord.value()).contains("\"processKey\":\"A01\"");

        if (paused) {
            JobPausedEvent jobPausedEvent = objectMapper.readValue(consumerRecord.value(), JobPausedEvent.class);
            assertThat(jobPausedEvent.getMessageKey()).isNotNull();
        } else {
            JobScheduledEvent jobScheduledEvent = objectMapper.readValue(consumerRecord.value(), JobScheduledEvent.class);
            assertThat(jobScheduledEvent.getMessageKey()).isNotNull();
        }
    }

    @Test
    void testDeserializeException() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testDeserializeException START", getClass().getName());
        // act
        produce("x", TOPIC_accepted);
        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_accepted_ERR);
        assertThat(consumerRecord.value()).contains("Exception");
    }

    @Test
    void testRuntimeException() throws JsonProcessingException, InterruptedException {

        LOGGER.info("{} testRuntimeException START", getClass().getName());
        // act
        JobAcceptedEvent event = new JobAcceptedEvent(); // id is null
        produce(event, TOPIC_accepted);
        // assert
        ConsumerRecord<String, String> consumerRecord = pollTopic(TOPIC_accepted_ERR);
        assertThat(consumerRecord.value()).contains("Exception");
    }
}
