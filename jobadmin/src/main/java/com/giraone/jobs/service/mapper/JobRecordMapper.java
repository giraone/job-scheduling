package com.giraone.jobs.service.mapper;

import com.giraone.jobs.domain.JobRecord;
import com.giraone.jobs.domain.Process;
import com.giraone.jobs.service.dto.JobRecordDTO;
import com.giraone.jobs.service.dto.ProcessDTO;
import com.github.f4b6a3.tsid.Tsid;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link JobRecord} and its DTO {@link JobRecordDTO}.
 */
@Mapper(componentModel = "spring")
public interface JobRecordMapper extends EntityMapper<JobRecordDTO, JobRecord> {

    @Mapping(target = "process", source = "process") // ADAPTED
    @Mapping(target = "id", source = "id", qualifiedByName = "tsid") // ADAPTED
    JobRecordDTO toDto(JobRecord s);

    @Named("processId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ProcessDTO toDtoProcessId(Process process);

    // ADAPTED
    @Named("tsid")
    static String longToString(long id) {
        return Tsid.from(id).toString();
    }

    // ADAPTED
    @Named("tsid")
    static long stringToLong(String id) {
        return Tsid.from(id).toLong();
    }
}
