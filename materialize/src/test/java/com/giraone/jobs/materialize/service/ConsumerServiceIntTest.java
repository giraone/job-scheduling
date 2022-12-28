package com.giraone.jobs.materialize.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobStatusChangedEvent;
import com.giraone.jobs.materialize.common.ObjectMapperBuilder;
import com.giraone.jobs.materialize.config.ApplicationProperties;
import com.giraone.jobs.materialize.persistence.JobRecord;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import org.assertj.core.data.TemporalUnitOffset;
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
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.data.relational.core.query.Query.query;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // because init() needs ConsumerService
class ConsumerServiceIntTest extends AbstractKafkaIntTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerServiceIntTest.class);
    private static final TemporalUnitOffset toleratedInstantOffset = within(1, ChronoUnit.MILLIS);
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

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
    void passOneNewEvent() throws Exception {

        r2dbcEntityTemplate.delete(JobRecord.class).matching(query(Criteria.empty())).all().block();
        assertThat(r2dbcEntityTemplate.select(JobRecord.class).count().block()).isEqualTo(0L);

        Tsid id = TsidCreator.getTsid256();
        Instant now = Instant.now();
        Instant jobAcceptedTimestamp = now.minusSeconds(10);
        JobAcceptedEvent event = new JobAcceptedEvent(id.toString(), "V001", jobAcceptedTimestamp);
        String jsonEvent = objectMapper.writeValueAsString(event);
        String topic = applicationProperties.getTopicInsert();
        String messageKey = event.getId();

        ReactiveKafkaProducerTemplate<String, String> template = new ReactiveKafkaProducerTemplate<>(senderOptions);
        template.send(topic, messageKey, jsonEvent)
            .doOnSuccess(senderResult -> LOGGER.info("Sent event {} to topic {} with offset : {}",
                jsonEvent, topic, senderResult.recordMetadata().offset()))
            .block();

        // We have to wait some time. We use at least the producer request timeout.
        Thread.sleep(requestTimeoutMillis);

        LOGGER.debug("Start selecting from StateRecord");
        JobRecord record = r2dbcEntityTemplate.select(JobRecord.class).all().blockFirst();
        assertThat(record).isNotNull();
        assertThat(record.getId()).isEqualTo(id.toLong());
        assertThat(record.getStatus()).isEqualTo("ACCEPTED");
        assertThat(record.getJobAcceptedTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(record.getLastEventTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(record.getLastRecordUpdateTimestamp()).isCloseTo(now, within(requestTimeoutMillis * 2, ChronoUnit.MILLIS));
    }

    @Test
    void passOneUpdateEvent() throws Exception {

        r2dbcEntityTemplate.delete(JobRecord.class).matching(query(Criteria.empty())).all().block();
        assertThat(r2dbcEntityTemplate.select(JobRecord.class).count().block()).isEqualTo(0L);

        Tsid id = TsidCreator.getTsid256();
        Instant now = Instant.now();
        Instant jobAcceptedTimestamp = now.minusSeconds(10);
        Instant eventTimestamp = now.minusSeconds(1);
        JobStatusChangedEvent event = new JobStatusChangedEvent(id.toString(), "V001", jobAcceptedTimestamp, eventTimestamp, "SCHEDULED");
        String jsonEvent = objectMapper.writeValueAsString(event);
        String topic = applicationProperties.getTopicsUpdate();

        ReactiveKafkaProducerTemplate<String, String> template = new ReactiveKafkaProducerTemplate<>(senderOptions);
        template.send(topic, jsonEvent)
            .doOnSuccess(senderResult -> LOGGER.info("sent {} partition={}, offset={}", jsonEvent,
                senderResult.recordMetadata().partition(), senderResult.recordMetadata().offset()))
            .block();

        // We have to wait some time. We use at least the producer request timeout.
        Thread.sleep(requestTimeoutMillis);

        LOGGER.debug("Start selecting from StateRecord");
        JobRecord record = r2dbcEntityTemplate.select(JobRecord.class).all().blockFirst();
        assertThat(record).isNotNull();
        assertThat(record.getId()).isEqualTo(id.toLong());
        assertThat(record.getStatus()).isEqualTo("SCHEDULED");
        assertThat(record.getJobAcceptedTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(record.getLastEventTimestamp()).isCloseTo(eventTimestamp, toleratedInstantOffset);
        assertThat(record.getLastRecordUpdateTimestamp()).isCloseTo(now, within(requestTimeoutMillis * 2, ChronoUnit.MILLIS));
    }
}
