package com.giraone.jobs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.giraone.jobs.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class JobRecordTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(JobRecord.class);
        JobRecord jobRecord1 = new JobRecord();
        jobRecord1.setId(1L);
        JobRecord jobRecord2 = new JobRecord();
        jobRecord2.setId(jobRecord1.getId());
        assertThat(jobRecord1).isEqualTo(jobRecord2);
        jobRecord2.setId(2L);
        assertThat(jobRecord1).isNotEqualTo(jobRecord2);
        jobRecord1.setId(null);
        assertThat(jobRecord1).isNotEqualTo(jobRecord2);
    }
}
