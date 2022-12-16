package com.giraone.jobs.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.giraone.jobs.IntegrationTest;
import com.giraone.jobs.domain.JobRecord;
import com.giraone.jobs.domain.Process;
import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import com.giraone.jobs.repository.JobRecordRepository;
import com.giraone.jobs.service.dto.JobRecordDTO;
import com.giraone.jobs.service.mapper.JobRecordMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link JobRecordResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class JobRecordResourceIT {

    private static final Instant DEFAULT_JOB_ACCEPTED_TIMESTAMP = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOB_ACCEPTED_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_EVENT_TIMESTAMP = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_EVENT_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_RECORD_UPDATE_TIMESTAMP = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_RECORD_UPDATE_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final JobStatusEnum DEFAULT_STATUS = JobStatusEnum.ACCEPTED;
    private static final JobStatusEnum UPDATED_STATUS = JobStatusEnum.SCHEDULED;

    private static final String DEFAULT_PAUSED_BUCKET_KEY = "AAAAAAAAAA";
    private static final String UPDATED_PAUSED_BUCKET_KEY = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/job-records";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private JobRecordRepository jobRecordRepository;

    @Autowired
    private JobRecordMapper jobRecordMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restJobRecordMockMvc;

    private JobRecord jobRecord;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JobRecord createEntity(EntityManager em) {
        JobRecord jobRecord = new JobRecord()
            .jobAcceptedTimestamp(DEFAULT_JOB_ACCEPTED_TIMESTAMP)
            .lastEventTimestamp(DEFAULT_LAST_EVENT_TIMESTAMP)
            .lastRecordUpdateTimestamp(DEFAULT_LAST_RECORD_UPDATE_TIMESTAMP)
            .status(DEFAULT_STATUS)
            .pausedBucketKey(DEFAULT_PAUSED_BUCKET_KEY);
        // Add required entity
        Process process;
        if (TestUtil.findAll(em, Process.class).isEmpty()) {
            process = ProcessResourceIT.createEntity(em);
            em.persist(process);
            em.flush();
        } else {
            process = TestUtil.findAll(em, Process.class).get(0);
        }
        jobRecord.setProcess(process);
        return jobRecord;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JobRecord createUpdatedEntity(EntityManager em) {
        JobRecord jobRecord = new JobRecord()
            .jobAcceptedTimestamp(UPDATED_JOB_ACCEPTED_TIMESTAMP)
            .lastEventTimestamp(UPDATED_LAST_EVENT_TIMESTAMP)
            .lastRecordUpdateTimestamp(UPDATED_LAST_RECORD_UPDATE_TIMESTAMP)
            .status(UPDATED_STATUS)
            .pausedBucketKey(UPDATED_PAUSED_BUCKET_KEY);
        // Add required entity
        Process process;
        if (TestUtil.findAll(em, Process.class).isEmpty()) {
            process = ProcessResourceIT.createUpdatedEntity(em);
            em.persist(process);
            em.flush();
        } else {
            process = TestUtil.findAll(em, Process.class).get(0);
        }
        jobRecord.setProcess(process);
        return jobRecord;
    }

    @BeforeEach
    public void initTest() {
        jobRecord = createEntity(em);
    }

    @Test
    @Transactional
    void createJobRecord() throws Exception {
        int databaseSizeBeforeCreate = jobRecordRepository.findAll().size();
        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);
        restJobRecordMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isCreated());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeCreate + 1);
        JobRecord testJobRecord = jobRecordList.get(jobRecordList.size() - 1);
        assertThat(testJobRecord.getJobAcceptedTimestamp()).isEqualTo(DEFAULT_JOB_ACCEPTED_TIMESTAMP);
        assertThat(testJobRecord.getLastEventTimestamp()).isEqualTo(DEFAULT_LAST_EVENT_TIMESTAMP);
        assertThat(testJobRecord.getLastRecordUpdateTimestamp()).isEqualTo(DEFAULT_LAST_RECORD_UPDATE_TIMESTAMP);
        assertThat(testJobRecord.getStatus()).isEqualTo(DEFAULT_STATUS);
        assertThat(testJobRecord.getPausedBucketKey()).isEqualTo(DEFAULT_PAUSED_BUCKET_KEY);
    }

    @Test
    @Transactional
    void createJobRecordWithExistingId() throws Exception {
        // Create the JobRecord with an existing ID
        jobRecord.setId(1L);
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        int databaseSizeBeforeCreate = jobRecordRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restJobRecordMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isBadRequest());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkJobAcceptedTimestampIsRequired() throws Exception {
        int databaseSizeBeforeTest = jobRecordRepository.findAll().size();
        // set the field null
        jobRecord.setJobAcceptedTimestamp(null);

        // Create the JobRecord, which fails.
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        restJobRecordMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isBadRequest());

        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLastEventTimestampIsRequired() throws Exception {
        int databaseSizeBeforeTest = jobRecordRepository.findAll().size();
        // set the field null
        jobRecord.setLastEventTimestamp(null);

        // Create the JobRecord, which fails.
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        restJobRecordMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isBadRequest());

        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLastRecordUpdateTimestampIsRequired() throws Exception {
        int databaseSizeBeforeTest = jobRecordRepository.findAll().size();
        // set the field null
        jobRecord.setLastRecordUpdateTimestamp(null);

        // Create the JobRecord, which fails.
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        restJobRecordMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isBadRequest());

        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        int databaseSizeBeforeTest = jobRecordRepository.findAll().size();
        // set the field null
        jobRecord.setStatus(null);

        // Create the JobRecord, which fails.
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        restJobRecordMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isBadRequest());

        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllJobRecords() throws Exception {
        // Initialize the database
        jobRecordRepository.saveAndFlush(jobRecord);

        // Get all the jobRecordList
        restJobRecordMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(jobRecord.getId().intValue())))
            .andExpect(jsonPath("$.[*].jobAcceptedTimestamp").value(hasItem(DEFAULT_JOB_ACCEPTED_TIMESTAMP.toString())))
            .andExpect(jsonPath("$.[*].lastEventTimestamp").value(hasItem(DEFAULT_LAST_EVENT_TIMESTAMP.toString())))
            .andExpect(jsonPath("$.[*].lastRecordUpdateTimestamp").value(hasItem(DEFAULT_LAST_RECORD_UPDATE_TIMESTAMP.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].pausedBucketKey").value(hasItem(DEFAULT_PAUSED_BUCKET_KEY)));
    }

    @Test
    @Transactional
    void getJobRecord() throws Exception {
        // Initialize the database
        jobRecordRepository.saveAndFlush(jobRecord);

        // Get the jobRecord
        restJobRecordMockMvc
            .perform(get(ENTITY_API_URL_ID, jobRecord.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(jobRecord.getId().intValue()))
            .andExpect(jsonPath("$.jobAcceptedTimestamp").value(DEFAULT_JOB_ACCEPTED_TIMESTAMP.toString()))
            .andExpect(jsonPath("$.lastEventTimestamp").value(DEFAULT_LAST_EVENT_TIMESTAMP.toString()))
            .andExpect(jsonPath("$.lastRecordUpdateTimestamp").value(DEFAULT_LAST_RECORD_UPDATE_TIMESTAMP.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.pausedBucketKey").value(DEFAULT_PAUSED_BUCKET_KEY));
    }

    @Test
    @Transactional
    void getNonExistingJobRecord() throws Exception {
        // Get the jobRecord
        restJobRecordMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewJobRecord() throws Exception {
        // Initialize the database
        jobRecordRepository.saveAndFlush(jobRecord);

        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();

        // Update the jobRecord
        JobRecord updatedJobRecord = jobRecordRepository.findById(jobRecord.getId()).get();
        // Disconnect from session so that the updates on updatedJobRecord are not directly saved in db
        em.detach(updatedJobRecord);
        updatedJobRecord
            .jobAcceptedTimestamp(UPDATED_JOB_ACCEPTED_TIMESTAMP)
            .lastEventTimestamp(UPDATED_LAST_EVENT_TIMESTAMP)
            .lastRecordUpdateTimestamp(UPDATED_LAST_RECORD_UPDATE_TIMESTAMP)
            .status(UPDATED_STATUS)
            .pausedBucketKey(UPDATED_PAUSED_BUCKET_KEY);
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(updatedJobRecord);

        restJobRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, jobRecordDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(jobRecordDTO))
            )
            .andExpect(status().isOk());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
        JobRecord testJobRecord = jobRecordList.get(jobRecordList.size() - 1);
        assertThat(testJobRecord.getJobAcceptedTimestamp()).isEqualTo(UPDATED_JOB_ACCEPTED_TIMESTAMP);
        assertThat(testJobRecord.getLastEventTimestamp()).isEqualTo(UPDATED_LAST_EVENT_TIMESTAMP);
        assertThat(testJobRecord.getLastRecordUpdateTimestamp()).isEqualTo(UPDATED_LAST_RECORD_UPDATE_TIMESTAMP);
        assertThat(testJobRecord.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testJobRecord.getPausedBucketKey()).isEqualTo(UPDATED_PAUSED_BUCKET_KEY);
    }

    @Test
    @Transactional
    void putNonExistingJobRecord() throws Exception {
        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();
        jobRecord.setId(count.incrementAndGet());

        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restJobRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, jobRecordDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(jobRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchJobRecord() throws Exception {
        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();
        jobRecord.setId(count.incrementAndGet());

        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restJobRecordMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(jobRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamJobRecord() throws Exception {
        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();
        jobRecord.setId(count.incrementAndGet());

        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restJobRecordMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(jobRecordDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateJobRecordWithPatch() throws Exception {
        // Initialize the database
        jobRecordRepository.saveAndFlush(jobRecord);

        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();

        // Update the jobRecord using partial update
        JobRecord partialUpdatedJobRecord = new JobRecord();
        partialUpdatedJobRecord.setId(jobRecord.getId());

        partialUpdatedJobRecord
            .jobAcceptedTimestamp(UPDATED_JOB_ACCEPTED_TIMESTAMP)
            .status(UPDATED_STATUS)
            .pausedBucketKey(UPDATED_PAUSED_BUCKET_KEY);

        restJobRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedJobRecord.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedJobRecord))
            )
            .andExpect(status().isOk());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
        JobRecord testJobRecord = jobRecordList.get(jobRecordList.size() - 1);
        assertThat(testJobRecord.getJobAcceptedTimestamp()).isEqualTo(UPDATED_JOB_ACCEPTED_TIMESTAMP);
        assertThat(testJobRecord.getLastEventTimestamp()).isEqualTo(DEFAULT_LAST_EVENT_TIMESTAMP);
        assertThat(testJobRecord.getLastRecordUpdateTimestamp()).isEqualTo(DEFAULT_LAST_RECORD_UPDATE_TIMESTAMP);
        assertThat(testJobRecord.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testJobRecord.getPausedBucketKey()).isEqualTo(UPDATED_PAUSED_BUCKET_KEY);
    }

    @Test
    @Transactional
    void fullUpdateJobRecordWithPatch() throws Exception {
        // Initialize the database
        jobRecordRepository.saveAndFlush(jobRecord);

        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();

        // Update the jobRecord using partial update
        JobRecord partialUpdatedJobRecord = new JobRecord();
        partialUpdatedJobRecord.setId(jobRecord.getId());

        partialUpdatedJobRecord
            .jobAcceptedTimestamp(UPDATED_JOB_ACCEPTED_TIMESTAMP)
            .lastEventTimestamp(UPDATED_LAST_EVENT_TIMESTAMP)
            .lastRecordUpdateTimestamp(UPDATED_LAST_RECORD_UPDATE_TIMESTAMP)
            .status(UPDATED_STATUS)
            .pausedBucketKey(UPDATED_PAUSED_BUCKET_KEY);

        restJobRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedJobRecord.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedJobRecord))
            )
            .andExpect(status().isOk());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
        JobRecord testJobRecord = jobRecordList.get(jobRecordList.size() - 1);
        assertThat(testJobRecord.getJobAcceptedTimestamp()).isEqualTo(UPDATED_JOB_ACCEPTED_TIMESTAMP);
        assertThat(testJobRecord.getLastEventTimestamp()).isEqualTo(UPDATED_LAST_EVENT_TIMESTAMP);
        assertThat(testJobRecord.getLastRecordUpdateTimestamp()).isEqualTo(UPDATED_LAST_RECORD_UPDATE_TIMESTAMP);
        assertThat(testJobRecord.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testJobRecord.getPausedBucketKey()).isEqualTo(UPDATED_PAUSED_BUCKET_KEY);
    }

    @Test
    @Transactional
    void patchNonExistingJobRecord() throws Exception {
        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();
        jobRecord.setId(count.incrementAndGet());

        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restJobRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, jobRecordDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(jobRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchJobRecord() throws Exception {
        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();
        jobRecord.setId(count.incrementAndGet());

        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restJobRecordMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(jobRecordDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamJobRecord() throws Exception {
        int databaseSizeBeforeUpdate = jobRecordRepository.findAll().size();
        jobRecord.setId(count.incrementAndGet());

        // Create the JobRecord
        JobRecordDTO jobRecordDTO = jobRecordMapper.toDto(jobRecord);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restJobRecordMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(jobRecordDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the JobRecord in the database
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteJobRecord() throws Exception {
        // Initialize the database
        jobRecordRepository.saveAndFlush(jobRecord);

        int databaseSizeBeforeDelete = jobRecordRepository.findAll().size();

        // Delete the jobRecord
        restJobRecordMockMvc
            .perform(delete(ENTITY_API_URL_ID, jobRecord.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<JobRecord> jobRecordList = jobRecordRepository.findAll();
        assertThat(jobRecordList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
