package com.giraone.jobs.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.giraone.jobs.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class JobRecordDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(JobRecordDTO.class);
        JobRecordDTO jobRecordDTO1 = new JobRecordDTO();
        jobRecordDTO1.setId(1L);
        JobRecordDTO jobRecordDTO2 = new JobRecordDTO();
        assertThat(jobRecordDTO1).isNotEqualTo(jobRecordDTO2);
        jobRecordDTO2.setId(jobRecordDTO1.getId());
        assertThat(jobRecordDTO1).isEqualTo(jobRecordDTO2);
        jobRecordDTO2.setId(2L);
        assertThat(jobRecordDTO1).isNotEqualTo(jobRecordDTO2);
        jobRecordDTO1.setId(null);
        assertThat(jobRecordDTO1).isNotEqualTo(jobRecordDTO2);
    }
}
