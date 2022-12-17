package com.giraone.jobs.repository;

import com.giraone.jobs.domain.JobRecord;
import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import io.micrometer.core.annotation.Timed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the JobRecord entity.
 */
@SuppressWarnings("unused")
@Repository
public interface JobRecordRepository extends JpaRepository<JobRecord, Long> {

    @Timed
    @Query("SELECT distinct j FROM JobRecord j where j.status = :status")
    Page<JobRecord> findByStatus(@Param("status") JobStatusEnum status, Pageable pageable);

    @Timed
    @Query("SELECT distinct j FROM JobRecord j where j.process.id = :processId")
    Page<JobRecord> findByProcessId(@Param("processId") long processId, Pageable pageable);

    @Timed
    @Query("SELECT distinct j FROM JobRecord j where j.process.key = :processKey")
    Page<JobRecord> findByProcessKey(@Param("processKey") String processKey, Pageable pageable);

    @Timed
    @Query("SELECT distinct j FROM JobRecord j where j.status = :status AND j.process.id = :processId")
    Page<JobRecord> findByStatusAndProcessId(@Param("status") JobStatusEnum status, @Param("processId") long processId, Pageable pageable);

    @Timed
    @Query("SELECT distinct j FROM JobRecord j where j.status = :status AND j.process.key = :processKey")
    Page<JobRecord> findByStatusAndProcessKey(@Param("status") JobStatusEnum status, @Param("processKey") String processKey, Pageable pageable);
}
