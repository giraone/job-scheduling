package com.giraone.jobs.service;

import com.giraone.jobs.domain.JobRecord;
import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import com.giraone.jobs.repository.JobRecordRepository;
import com.giraone.jobs.service.dto.JobRecordDTO;
import com.giraone.jobs.service.mapper.JobRecordMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link JobRecord}.
 */
@Service
@Transactional
public class JobRecordService {

    private final Logger log = LoggerFactory.getLogger(JobRecordService.class);

    private final JobRecordRepository jobRecordRepository;

    private final JobRecordMapper jobRecordMapper;

    public JobRecordService(JobRecordRepository jobRecordRepository, JobRecordMapper jobRecordMapper) {
        this.jobRecordRepository = jobRecordRepository;
        this.jobRecordMapper = jobRecordMapper;
    }

    /**
     * Save a jobRecord.
     *
     * @param jobRecordDTO the entity to save.
     * @return the persisted entity.
     */
    public JobRecordDTO save(JobRecordDTO jobRecordDTO) {
        log.debug("Request to save JobRecord : {}", jobRecordDTO);
        JobRecord jobRecord = jobRecordMapper.toEntity(jobRecordDTO);
        jobRecord = jobRecordRepository.save(jobRecord);
        return jobRecordMapper.toDto(jobRecord);
    }

    /**
     * Update a jobRecord.
     *
     * @param jobRecordDTO the entity to save.
     * @return the persisted entity.
     */
    public JobRecordDTO update(JobRecordDTO jobRecordDTO) {
        log.debug("Request to save JobRecord : {}", jobRecordDTO);
        JobRecord jobRecord = jobRecordMapper.toEntity(jobRecordDTO);
        jobRecord = jobRecordRepository.save(jobRecord);
        return jobRecordMapper.toDto(jobRecord);
    }

    /**
     * Partially update a jobRecord.
     *
     * @param jobRecordDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<JobRecordDTO> partialUpdate(JobRecordDTO jobRecordDTO) {
        log.debug("Request to partially update JobRecord : {}", jobRecordDTO);

        return jobRecordRepository
            .findById(jobRecordDTO.getId())
            .map(existingJobRecord -> {
                jobRecordMapper.partialUpdate(existingJobRecord, jobRecordDTO);

                return existingJobRecord;
            })
            .map(jobRecordRepository::save)
            .map(jobRecordMapper::toDto);
    }

    /**
     * Get all the jobRecords.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<JobRecordDTO> findAll(Pageable pageable) {
        log.debug("Request to get all JobRecords");
        return jobRecordRepository.findAll(pageable).map(jobRecordMapper::toDto);
    }

    // ADAPTED
    /**
     * Get all the (filtered) jobRecords.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<JobRecordDTO> findFiltered(JobStatusEnum status, Long processId, Pageable pageable) {
        log.debug("Request to get all filtered JobRecords");
        Page<JobRecord> records;
        if (processId != null && processId != 0) {
            if (status != null) {
                records = jobRecordRepository.findByStatusAndProcessId(status, processId, pageable);
            } else {
                records = jobRecordRepository.findByProcessId(processId, pageable);
            }
        } else if (status != null) {
            records = jobRecordRepository.findByStatus(status, pageable);
        } else {
            records = jobRecordRepository.findAll(pageable);
        }
        log.debug("Request to get all filtered JobRecords returned {} of {} records", records.getSize(), records.getTotalElements());
        final Page<JobRecordDTO> ret = records.map(jobRecordMapper::toDto);
        return ret;
    }

    /**
     * Get one jobRecord by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<JobRecordDTO> findOne(Long id) {
        log.debug("Request to get JobRecord : {}", id);
        return jobRecordRepository.findById(id).map(jobRecordMapper::toDto);
    }

    /**
     * Delete the jobRecord by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete JobRecord : {}", id);
        jobRecordRepository.deleteById(id);
    }

    // ADAPTED
    public void deleteAll() {
        log.debug("Request to delete all JobRecords");
        jobRecordRepository.deleteAll();
    }
}
