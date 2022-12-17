package com.giraone.jobs.service;

import com.giraone.jobs.domain.Process;
import com.giraone.jobs.repository.ProcessRepository;
import com.giraone.jobs.service.dto.ProcessDTO;
import com.giraone.jobs.service.mapper.ProcessMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Process}.
 */
@Service
@Transactional
public class ProcessService {

    private final Logger log = LoggerFactory.getLogger(ProcessService.class);

    private final ProcessRepository processRepository;

    private final ProcessMapper processMapper;

    public ProcessService(ProcessRepository processRepository, ProcessMapper processMapper) {
        this.processRepository = processRepository;
        this.processMapper = processMapper;
    }

    /**
     * Save a process.
     *
     * @param processDTO the entity to save.
     * @return the persisted entity.
     */
    public ProcessDTO save(ProcessDTO processDTO) {
        log.debug("Request to save Process : {}", processDTO);
        Process process = processMapper.toEntity(processDTO);
        process = processRepository.save(process);
        return processMapper.toDto(process);
    }

    /**
     * Update a process.
     *
     * @param processDTO the entity to save.
     * @return the persisted entity.
     */
    public ProcessDTO update(ProcessDTO processDTO) {
        log.debug("Request to update Process : {}", processDTO);
        Process process = processMapper.toEntity(processDTO);
        process = processRepository.save(process);
        return processMapper.toDto(process);
    }

    /**
     * Partially update a process.
     *
     * @param processDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProcessDTO> partialUpdate(ProcessDTO processDTO) {
        log.debug("Request to partially update Process : {}", processDTO);

        return processRepository
            .findById(processDTO.getId())
            .map(existingProcess -> {
                processMapper.partialUpdate(existingProcess, processDTO);

                return existingProcess;
            })
            .map(processRepository::save)
            .map(processMapper::toDto);
    }

    /**
     * Get all the processes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ProcessDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Processes");
        return processRepository.findAll(pageable).map(processMapper::toDto);
    }

    /**
     * Get one process by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProcessDTO> findOne(Long id) {
        log.debug("Request to get Process : {}", id);
        return processRepository.findById(id).map(processMapper::toDto);
    }

    /**
     * Delete the process by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Process : {}", id);
        processRepository.deleteById(id);
    }
}
