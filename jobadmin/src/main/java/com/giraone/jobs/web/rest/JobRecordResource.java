package com.giraone.jobs.web.rest;

import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import com.giraone.jobs.repository.JobRecordRepository;
import com.giraone.jobs.service.JobRecordService;
import com.giraone.jobs.service.dto.JobRecordDTO;
import com.giraone.jobs.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.giraone.jobs.domain.JobRecord}.
 */
@RestController
@RequestMapping("/api")
public class JobRecordResource {

    private final Logger log = LoggerFactory.getLogger(JobRecordResource.class);

    private static final String ENTITY_NAME = "jobRecord";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final JobRecordService jobRecordService;

    private final JobRecordRepository jobRecordRepository;

    public JobRecordResource(JobRecordService jobRecordService, JobRecordRepository jobRecordRepository) {
        this.jobRecordService = jobRecordService;
        this.jobRecordRepository = jobRecordRepository;
    }

    /**
     * {@code POST  /job-records} : Create a new jobRecord.
     *
     * @param jobRecordDTO the jobRecordDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new jobRecordDTO, or with status {@code 400 (Bad Request)} if the jobRecord has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/job-records")
    public ResponseEntity<JobRecordDTO> createJobRecord(@Valid @RequestBody JobRecordDTO jobRecordDTO) throws URISyntaxException {
        log.debug("REST request to save JobRecord : {}", jobRecordDTO);
        if (jobRecordDTO.getId() != null) {
            throw new BadRequestAlertException("A new jobRecord cannot already have an ID", ENTITY_NAME, "idexists");
        }
        JobRecordDTO result = jobRecordService.save(jobRecordDTO);
        return ResponseEntity
            .created(new URI("/api/job-records/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /job-records/:id} : Updates an existing jobRecord.
     *
     * @param id the id of the jobRecordDTO to save.
     * @param jobRecordDTO the jobRecordDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated jobRecordDTO,
     * or with status {@code 400 (Bad Request)} if the jobRecordDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the jobRecordDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/job-records/{id}")
    public ResponseEntity<JobRecordDTO> updateJobRecord(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody JobRecordDTO jobRecordDTO
    ) throws URISyntaxException {
        log.debug("REST request to update JobRecord : {}, {}", id, jobRecordDTO);
        if (jobRecordDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, jobRecordDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!jobRecordRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        JobRecordDTO result = jobRecordService.update(jobRecordDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, jobRecordDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /job-records/:id} : Partial updates given fields of an existing jobRecord, field will ignore if it is null
     *
     * @param id the id of the jobRecordDTO to save.
     * @param jobRecordDTO the jobRecordDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated jobRecordDTO,
     * or with status {@code 400 (Bad Request)} if the jobRecordDTO is not valid,
     * or with status {@code 404 (Not Found)} if the jobRecordDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the jobRecordDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/job-records/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<JobRecordDTO> partialUpdateJobRecord(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody JobRecordDTO jobRecordDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update JobRecord partially : {}, {}", id, jobRecordDTO);
        if (jobRecordDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, jobRecordDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!jobRecordRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<JobRecordDTO> result = jobRecordService.partialUpdate(jobRecordDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, jobRecordDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /job-records} : get all the jobRecords.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of jobRecords in body.
     */
    @GetMapping("/job-records")
    public ResponseEntity<List<JobRecordDTO>> getAllJobRecords(
        @RequestParam(required = false) JobStatusEnum status,
        @RequestParam(required = false) Long processId,
        @ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get a page of JobRecords status={} process={}", status, processId);
        Page<JobRecordDTO> page = jobRecordService.findFiltered(status, processId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /job-records/:id} : get the "id" jobRecord.
     *
     * @param id the id of the jobRecordDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the jobRecordDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/job-records/{id}")
    public ResponseEntity<JobRecordDTO> getJobRecord(@PathVariable Long id) {
        log.debug("REST request to get JobRecord : {}", id);
        Optional<JobRecordDTO> jobRecordDTO = jobRecordService.findOne(id);
        return ResponseUtil.wrapOrNotFound(jobRecordDTO);
    }

    /**
     * {@code DELETE  /job-records/:id} : delete the "id" jobRecord.
     *
     * @param id the id of the jobRecordDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/job-records/{id}")
    public ResponseEntity<Void> deleteJobRecord(@PathVariable Long id) {
        log.debug("REST request to delete JobRecord : {}", id);
        jobRecordService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code DELETE  /job-records} : delete all jobRecord.
     */
    @DeleteMapping("/job-records-delete-all")
    public ResponseEntity<Void> deleteAllJobRecord() {
        log.debug("REST request to delete all JobRecords");
        jobRecordService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
