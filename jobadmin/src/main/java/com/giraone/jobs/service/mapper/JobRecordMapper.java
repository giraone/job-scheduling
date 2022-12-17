package com.giraone.jobs.service.mapper;

import com.giraone.jobs.domain.JobRecord;
import com.giraone.jobs.domain.Process;
import com.giraone.jobs.service.dto.JobRecordDTO;
import com.giraone.jobs.service.dto.ProcessDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link JobRecord} and its DTO {@link JobRecordDTO}.
 */
@Mapper(componentModel = "spring")
public interface JobRecordMapper extends EntityMapper<JobRecordDTO, JobRecord> {
    @Mapping(target = "process", source = "process")
    JobRecordDTO toDto(JobRecord s);

    @Named("processId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ProcessDTO toDtoProcessId(Process process);
}
