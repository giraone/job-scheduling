package com.giraone.jobs.materialize.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobStatusChangedEvent;
import com.giraone.jobs.materialize.common.ObjectMapperBuilder;
import com.giraone.jobs.materialize.config.ApplicationProperties;
import com.giraone.jobs.materialize.persistence.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.annotation.DirtiesContext;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // because init() needs ConsumerService
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Need, so that new event is consumed before update event
class ConsumerServiceIntTest extends AbstractKafkaIntTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerServiceIntTest.class);
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);
    private static final Tsid id = TsidCreator.getTsid256();

    @Autowired
    ApplicationProperties applicationProperties;
    @Autowired
    R2dbcEntityTemplate r2dbcEntityTemplate;

    @BeforeEach
    protected void setUp() {
        LOGGER.debug("ConsumerServiceIntTest.setUp");
        this.waitForTopic("job-accepted", true);
    }

    @Test
    @Order(1)
    void passOneNewEvent() throws Exception {

        r2dbcEntityTemplate.delete(JobRecord.class).all().block();

        JobAcceptedEvent event = new JobAcceptedEvent(id.toString(), "V001", Instant.now(), Instant.now());
        String jsonEvent = objectMapper.writeValueAsString(event);
        String topic = applicationProperties.getTopicInsert();
        String messageKey = event.getId();

        ReactiveKafkaProducerTemplate<String, String> template = new ReactiveKafkaProducerTemplate<>(senderOptions);
        template.send(topic, messageKey, jsonEvent)
            .doOnSuccess(senderResult -> LOGGER.info("Sent event {} to topic {} with offset : {}",
                jsonEvent, topic, senderResult.recordMetadata().offset()))
            .block();

        LOGGER.debug("Start selecting from StateRecord");
        JobRecord record = r2dbcEntityTemplate.select(JobRecord.class).all().blockFirst();
        assertThat(record).isNotNull();
        assertThat(record.getId()).isEqualTo(id.toLong());
        assertThat(record.getStatus()).isEqualTo("ACCEPTED");
        assertThat(record.getJobAcceptedTimestamp()).isEqualTo(event.getEventTimestamp());
        assertThat(record.getLastEventTimestamp()).isNotNull();
        assertThat(record.getLastRecordUpdateTimestamp()).isNotNull();
    }

    @Test
    @Order(2)
    void passOneUpdateEvent() throws JsonProcessingException {

        r2dbcEntityTemplate.delete(JobRecord.class).all().block();

        JobStatusChangedEvent event = new JobStatusChangedEvent(id.toString(), "V001", Instant.now(), Instant.now(),"SCHEDULED");
        String jsonEvent = objectMapper.writeValueAsString(event);
        String topic = applicationProperties.getTopicsUpdate();

        try (ReactiveKafkaProducerTemplate<String, String> template = new ReactiveKafkaProducerTemplate<>(senderOptions)) {
            template.send(topic, jsonEvent)
                .doOnSuccess(senderResult -> LOGGER.info("sent {} offset : {}", jsonEvent, senderResult.recordMetadata().offset()))
                .as(StepVerifier::create)
                .then(() -> r2dbcEntityTemplate.select(JobRecord.class).all()
                    .as(StepVerifier::create)
                    .expectNextCount(1L)
                    .assertNext(record -> {
                        assertThat(record.getId()).isEqualTo(id.toLong());
                        assertThat(record.getStatus()).isEqualTo("scheduled");
                        assertThat(record.getJobAcceptedTimestamp()).isNotNull();
                        assertThat(record.getLastEventTimestamp()).isEqualTo(event.getEventTimestamp());
                        assertThat(record.getLastRecordUpdateTimestamp()).isNotNull();
                    })
                    .verifyComplete())
                .verifyComplete();
        }
    }
}
