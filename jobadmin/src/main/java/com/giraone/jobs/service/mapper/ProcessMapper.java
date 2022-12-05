package com.giraone.jobs.service.mapper;

import com.giraone.jobs.domain.Process;
import com.giraone.jobs.service.dto.ProcessDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Process} and its DTO {@link ProcessDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProcessMapper extends EntityMapper<ProcessDTO, Process> {}
