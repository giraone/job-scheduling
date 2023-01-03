package com.giraone.jobs.materialize.persistence;

import com.github.f4b6a3.tsid.TsidCreator;
import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.giraone.jobs.materialize.persistence.JobRecord.STATE_accepted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@DirtiesContext
class JobRecordRepositoryTest {

    private static final TemporalUnitOffset toleratedInstantOffset = within(1, ChronoUnit.MILLIS);

    @Autowired
    private JobRecordRepository jobRecordRepository;

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Test
    void findByIdForUpdate() {

        // arrange
        long id = TsidCreator.getTsid256().toLong();
        long processId = 1L;
        Instant lastEventTimestamp = Instant.now();
        Instant jobAcceptedTimestamp = lastEventTimestamp.minusSeconds(10);
        JobRecord jobRecord = new JobRecord(id, jobAcceptedTimestamp, lastEventTimestamp, processId);
        jobRecord = r2dbcEntityTemplate.insert(jobRecord).block();
        assertThat(jobRecord).isNotNull();
        Instant lastRecordUpdateTimestamp = jobRecord.getLastRecordUpdateTimestamp();
        assertThat(lastRecordUpdateTimestamp).isAfterOrEqualTo(lastEventTimestamp);

        // act
        JobRecord found = jobRecordRepository.findByIdForUpdate(id).block();

        // assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getJobAcceptedTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(found.getLastEventTimestamp()).isCloseTo(lastEventTimestamp, toleratedInstantOffset);
        assertThat(found.getLastRecordUpdateTimestamp()).isCloseTo(lastRecordUpdateTimestamp, toleratedInstantOffset);
        assertThat(found.getStatus()).isEqualTo(STATE_accepted);
        assertThat(found.getProcessId()).isEqualTo(processId);
    }

    @Test
    void insert() {

        // arrange
        long id = TsidCreator.getTsid256().toLong();
        long processId = 1L;
        String status = "SCHEDULED";
        Instant lastEventTimestamp = Instant.now();
        Instant jobAcceptedTimestamp = lastEventTimestamp.minusSeconds(10);
        Instant lastRecordUpdateTimestamp = lastEventTimestamp.minusSeconds(1);

        // act
        Long inserted = jobRecordRepository.insert(id, jobAcceptedTimestamp, lastEventTimestamp,
            lastRecordUpdateTimestamp, status, null, processId).block();

        // assert
        assertThat(inserted).isEqualTo(1);
        JobRecord found = jobRecordRepository.findByIdForUpdate(id).block();
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getJobAcceptedTimestamp()).isCloseTo(jobAcceptedTimestamp, toleratedInstantOffset);
        assertThat(found.getLastEventTimestamp()).isCloseTo(lastEventTimestamp, toleratedInstantOffset);
        assertThat(found.getLastRecordUpdateTimestamp()).isCloseTo(lastRecordUpdateTimestamp, toleratedInstantOffset);
        assertThat(found.getStatus()).isEqualTo(status);
        assertThat(found.getProcessId()).isEqualTo(processId);
    }

    @Test
    void insertIgnoreConflict() {

        // arrange
        long id = TsidCreator.getTsid256().toLong();
        long processId = 1L;
        String status = "SCHEDULED";
        Instant lastEventTimestamp = Instant.now();
        Instant jobAcceptedTimestamp = lastEventTimestamp.minusSeconds(10);
        Instant lastRecordUpdateTimestamp = lastEventTimestamp.minusSeconds(1);

        // act
        Long inserted1 = jobRecordRepository.insertIgnoreConflict(id, jobAcceptedTimestamp, lastEventTimestamp,
            lastRecordUpdateTimestamp, status, null, processId).block();

        // assert
        assertThat(inserted1).isEqualTo(1);

        // act
        Long inserted2 = jobRecordRepository.insertIgnoreConflict(id, jobAcceptedTimestamp, lastEventTimestamp,
            lastRecordUpdateTimestamp, status, null, processId).block();

        // assert
        assertThat(inserted2).isEqualTo(0);
    }

    @Test
    void insertOnConflictUpdate() {

        // arrange
        long id = TsidCreator.getTsid256().toLong();
        long processId = 1L;
        String status1 = "SCHEDULED";
        String status2 = "COMPLETED";
        Instant lastEventTimestamp = Instant.now();
        Instant jobAcceptedTimestamp = lastEventTimestamp.minusSeconds(10);
        Instant lastRecordUpdateTimestamp1 = lastEventTimestamp.minusSeconds(2);
        Instant lastRecordUpdateTimestamp2 = lastEventTimestamp.minusSeconds(1);

        // act
        Long inserted1 = jobRecordRepository.insertOnConflictUpdate(id, jobAcceptedTimestamp, lastEventTimestamp,
            lastRecordUpdateTimestamp1, status1, null, processId).block();

        // assert
        assertThat(inserted1).isEqualTo(1);

        // act
        Long inserted2 = jobRecordRepository.insertOnConflictUpdate(id, jobAcceptedTimestamp, lastEventTimestamp,
            lastRecordUpdateTimestamp2, status2, null, processId).block();

        // assert
        assertThat(inserted2).isEqualTo(1);
    }
}