package com.giraone.jobs.repository;

import com.giraone.jobs.domain.JobRecord;
import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the JobRecord entity.
 */
@SuppressWarnings("unused")
@Repository
public interface JobRecordRepository extends JpaRepository<JobRecord, Long> {
    Page<JobRecord> findByStatus(JobStatusEnum status, Pageable pageable);
    Page<JobRecord> findByProcessId(String processId, Pageable pageable);
    Page<JobRecord> findByStatusAndProcessId(JobStatusEnum status, String processId, Pageable pageable);
}
