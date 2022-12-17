package com.giraone.jobs.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.giraone.jobs.IntegrationTest;
import com.giraone.jobs.domain.Process;
import com.giraone.jobs.domain.enumeration.ActivationEnum;
import com.giraone.jobs.repository.ProcessRepository;
import com.giraone.jobs.service.dto.ProcessDTO;
import com.giraone.jobs.service.mapper.ProcessMapper;
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
 * Integration tests for the {@link ProcessResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProcessResourceIT {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final ActivationEnum DEFAULT_ACTIVATION = ActivationEnum.ACTIVE;
    private static final ActivationEnum UPDATED_ACTIVATION = ActivationEnum.PAUSED;

    private static final String DEFAULT_AGENT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_AGENT_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_BUCKET_KEY_IF_PAUSED = "AAAAAAAAAA";
    private static final String UPDATED_BUCKET_KEY_IF_PAUSED = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/processes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private ProcessMapper processMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProcessMockMvc;

    private Process process;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Process createEntity(EntityManager em) {
        Process process = new Process()
            .key(DEFAULT_KEY)
            .name(DEFAULT_NAME)
            .activation(DEFAULT_ACTIVATION)
            .agentKey(DEFAULT_AGENT_KEY)
            .bucketKeyIfPaused(DEFAULT_BUCKET_KEY_IF_PAUSED);
        return process;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Process createUpdatedEntity(EntityManager em) {
        Process process = new Process()
            .key(UPDATED_KEY)
            .name(UPDATED_NAME)
            .activation(UPDATED_ACTIVATION)
            .agentKey(UPDATED_AGENT_KEY)
            .bucketKeyIfPaused(UPDATED_BUCKET_KEY_IF_PAUSED);
        return process;
    }

    @BeforeEach
    public void initTest() {
        process = createEntity(em);
    }

    @Test
    @Transactional
    void createProcess() throws Exception {
        int databaseSizeBeforeCreate = processRepository.findAll().size();
        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);
        restProcessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(processDTO)))
            .andExpect(status().isCreated());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeCreate + 1);
        Process testProcess = processList.get(processList.size() - 1);
        assertThat(testProcess.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testProcess.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testProcess.getActivation()).isEqualTo(DEFAULT_ACTIVATION);
        assertThat(testProcess.getAgentKey()).isEqualTo(DEFAULT_AGENT_KEY);
        assertThat(testProcess.getBucketKeyIfPaused()).isEqualTo(DEFAULT_BUCKET_KEY_IF_PAUSED);
    }

    @Test
    @Transactional
    void createProcessWithExistingId() throws Exception {
        // Create the Process with an existing ID
        process.setId(1L);
        ProcessDTO processDTO = processMapper.toDto(process);

        int databaseSizeBeforeCreate = processRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProcessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(processDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = processRepository.findAll().size();
        // set the field null
        process.setKey(null);

        // Create the Process, which fails.
        ProcessDTO processDTO = processMapper.toDto(process);

        restProcessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(processDTO)))
            .andExpect(status().isBadRequest());

        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = processRepository.findAll().size();
        // set the field null
        process.setName(null);

        // Create the Process, which fails.
        ProcessDTO processDTO = processMapper.toDto(process);

        restProcessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(processDTO)))
            .andExpect(status().isBadRequest());

        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActivationIsRequired() throws Exception {
        int databaseSizeBeforeTest = processRepository.findAll().size();
        // set the field null
        process.setActivation(null);

        // Create the Process, which fails.
        ProcessDTO processDTO = processMapper.toDto(process);

        restProcessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(processDTO)))
            .andExpect(status().isBadRequest());

        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllProcesses() throws Exception {
        // Initialize the database
        processRepository.saveAndFlush(process);

        // Get all the processList
        restProcessMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(process.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].activation").value(hasItem(DEFAULT_ACTIVATION.toString())))
            .andExpect(jsonPath("$.[*].agentKey").value(hasItem(DEFAULT_AGENT_KEY)))
            .andExpect(jsonPath("$.[*].bucketKeyIfPaused").value(hasItem(DEFAULT_BUCKET_KEY_IF_PAUSED)));
    }

    @Test
    @Transactional
    void getProcess() throws Exception {
        // Initialize the database
        processRepository.saveAndFlush(process);

        // Get the process
        restProcessMockMvc
            .perform(get(ENTITY_API_URL_ID, process.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(process.getId().intValue()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.activation").value(DEFAULT_ACTIVATION.toString()))
            .andExpect(jsonPath("$.agentKey").value(DEFAULT_AGENT_KEY))
            .andExpect(jsonPath("$.bucketKeyIfPaused").value(DEFAULT_BUCKET_KEY_IF_PAUSED));
    }

    @Test
    @Transactional
    void getNonExistingProcess() throws Exception {
        // Get the process
        restProcessMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingProcess() throws Exception {
        // Initialize the database
        processRepository.saveAndFlush(process);

        int databaseSizeBeforeUpdate = processRepository.findAll().size();

        // Update the process
        Process updatedProcess = processRepository.findById(process.getId()).get();
        // Disconnect from session so that the updates on updatedProcess are not directly saved in db
        em.detach(updatedProcess);
        updatedProcess
            .key(UPDATED_KEY)
            .name(UPDATED_NAME)
            .activation(UPDATED_ACTIVATION)
            .agentKey(UPDATED_AGENT_KEY)
            .bucketKeyIfPaused(UPDATED_BUCKET_KEY_IF_PAUSED);
        ProcessDTO processDTO = processMapper.toDto(updatedProcess);

        restProcessMockMvc
            .perform(
                put(ENTITY_API_URL_ID, processDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(processDTO))
            )
            .andExpect(status().isOk());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
        Process testProcess = processList.get(processList.size() - 1);
        assertThat(testProcess.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testProcess.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProcess.getActivation()).isEqualTo(UPDATED_ACTIVATION);
        assertThat(testProcess.getAgentKey()).isEqualTo(UPDATED_AGENT_KEY);
        assertThat(testProcess.getBucketKeyIfPaused()).isEqualTo(UPDATED_BUCKET_KEY_IF_PAUSED);
    }

    @Test
    @Transactional
    void putNonExistingProcess() throws Exception {
        int databaseSizeBeforeUpdate = processRepository.findAll().size();
        process.setId(count.incrementAndGet());

        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProcessMockMvc
            .perform(
                put(ENTITY_API_URL_ID, processDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(processDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProcess() throws Exception {
        int databaseSizeBeforeUpdate = processRepository.findAll().size();
        process.setId(count.incrementAndGet());

        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProcessMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(processDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProcess() throws Exception {
        int databaseSizeBeforeUpdate = processRepository.findAll().size();
        process.setId(count.incrementAndGet());

        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProcessMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(processDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProcessWithPatch() throws Exception {
        // Initialize the database
        processRepository.saveAndFlush(process);

        int databaseSizeBeforeUpdate = processRepository.findAll().size();

        // Update the process using partial update
        Process partialUpdatedProcess = new Process();
        partialUpdatedProcess.setId(process.getId());

        partialUpdatedProcess.agentKey(UPDATED_AGENT_KEY).bucketKeyIfPaused(UPDATED_BUCKET_KEY_IF_PAUSED);

        restProcessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProcess.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedProcess))
            )
            .andExpect(status().isOk());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
        Process testProcess = processList.get(processList.size() - 1);
        assertThat(testProcess.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testProcess.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testProcess.getActivation()).isEqualTo(DEFAULT_ACTIVATION);
        assertThat(testProcess.getAgentKey()).isEqualTo(UPDATED_AGENT_KEY);
        assertThat(testProcess.getBucketKeyIfPaused()).isEqualTo(UPDATED_BUCKET_KEY_IF_PAUSED);
    }

    @Test
    @Transactional
    void fullUpdateProcessWithPatch() throws Exception {
        // Initialize the database
        processRepository.saveAndFlush(process);

        int databaseSizeBeforeUpdate = processRepository.findAll().size();

        // Update the process using partial update
        Process partialUpdatedProcess = new Process();
        partialUpdatedProcess.setId(process.getId());

        partialUpdatedProcess
            .key(UPDATED_KEY)
            .name(UPDATED_NAME)
            .activation(UPDATED_ACTIVATION)
            .agentKey(UPDATED_AGENT_KEY)
            .bucketKeyIfPaused(UPDATED_BUCKET_KEY_IF_PAUSED);

        restProcessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProcess.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedProcess))
            )
            .andExpect(status().isOk());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
        Process testProcess = processList.get(processList.size() - 1);
        assertThat(testProcess.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testProcess.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProcess.getActivation()).isEqualTo(UPDATED_ACTIVATION);
        assertThat(testProcess.getAgentKey()).isEqualTo(UPDATED_AGENT_KEY);
        assertThat(testProcess.getBucketKeyIfPaused()).isEqualTo(UPDATED_BUCKET_KEY_IF_PAUSED);
    }

    @Test
    @Transactional
    void patchNonExistingProcess() throws Exception {
        int databaseSizeBeforeUpdate = processRepository.findAll().size();
        process.setId(count.incrementAndGet());

        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProcessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, processDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(processDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProcess() throws Exception {
        int databaseSizeBeforeUpdate = processRepository.findAll().size();
        process.setId(count.incrementAndGet());

        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProcessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(processDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProcess() throws Exception {
        int databaseSizeBeforeUpdate = processRepository.findAll().size();
        process.setId(count.incrementAndGet());

        // Create the Process
        ProcessDTO processDTO = processMapper.toDto(process);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProcessMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(processDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Process in the database
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProcess() throws Exception {
        // Initialize the database
        processRepository.saveAndFlush(process);

        int databaseSizeBeforeDelete = processRepository.findAll().size();

        // Delete the process
        restProcessMockMvc
            .perform(delete(ENTITY_API_URL_ID, process.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Process> processList = processRepository.findAll();
        assertThat(processList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
