package com.giraone.jobs.web.rest;

import com.giraone.jobs.repository.ProcessRepository;
import com.giraone.jobs.service.ProcessService;
import com.giraone.jobs.service.dto.ProcessDTO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.giraone.jobs.domain.Process}.
 */
@RestController
@RequestMapping("/api")
public class ProcessResource {

    private final Logger log = LoggerFactory.getLogger(ProcessResource.class);

    private static final String ENTITY_NAME = "process";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProcessService processService;

    private final ProcessRepository processRepository;

    public ProcessResource(ProcessService processService, ProcessRepository processRepository) {
        this.processService = processService;
        this.processRepository = processRepository;
    }

    /**
     * {@code POST  /processes} : Create a new process.
     *
     * @param processDTO the processDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new processDTO, or with status {@code 400 (Bad Request)} if the process has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/processes")
    public ResponseEntity<ProcessDTO> createProcess(@Valid @RequestBody ProcessDTO processDTO) throws URISyntaxException {
        log.debug("REST request to save Process : {}", processDTO);
        if (processDTO.getId() != null) {
            throw new BadRequestAlertException("A new process cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ProcessDTO result = processService.save(processDTO);
        return ResponseEntity
            .created(new URI("/api/processes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /processes/:id} : Updates an existing process.
     *
     * @param id the id of the processDTO to save.
     * @param processDTO the processDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated processDTO,
     * or with status {@code 400 (Bad Request)} if the processDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the processDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/processes/{id}")
    public ResponseEntity<ProcessDTO> updateProcess(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody ProcessDTO processDTO
    ) throws URISyntaxException {
        log.debug("REST request to update Process : {}, {}", id, processDTO);
        if (processDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, processDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        ProcessDTO result;
        if (!processRepository.existsById(id)) {
            result = processService.save(processDTO);
        } else {
            result = processService.update(processDTO);
        }

        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, processDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /processes/:id} : Partial updates given fields of an existing process, field will ignore if it is null
     *
     * @param id the id of the processDTO to save.
     * @param processDTO the processDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated processDTO,
     * or with status {@code 400 (Bad Request)} if the processDTO is not valid,
     * or with status {@code 404 (Not Found)} if the processDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the processDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/processes/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProcessDTO> partialUpdateProcess(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody ProcessDTO processDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update Process partially : {}, {}", id, processDTO);
        if (processDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, processDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!processRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProcessDTO> result = processService.partialUpdate(processDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, processDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /processes} : get all the processes.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of processes in body.
     */
    @GetMapping("/processes")
    public ResponseEntity<List<ProcessDTO>> getAllProcesses(@org.springdoc.api.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Processes");
        Page<ProcessDTO> page = processService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /processes/:id} : get the "id" process.
     *
     * @param id the id of the processDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the processDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/processes/{id}")
    public ResponseEntity<ProcessDTO> getProcess(@PathVariable String id) {
        log.debug("REST request to get Process : {}", id);
        Optional<ProcessDTO> processDTO = processService.findOne(id);
        return ResponseUtil.wrapOrNotFound(processDTO);
    }

    /**
     * {@code DELETE  /processes/:id} : delete the "id" process.
     *
     * @param id the id of the processDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/processes/{id}")
    public ResponseEntity<Void> deleteProcess(@PathVariable String id) {
        log.debug("REST request to delete Process : {}", id);
        processService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
