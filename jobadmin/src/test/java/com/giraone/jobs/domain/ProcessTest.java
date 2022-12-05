package com.giraone.jobs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.giraone.jobs.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProcessTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Process.class);
        Process process1 = new Process();
        process1.setId("001");
        Process process2 = new Process();
        process2.setId(process1.getId());
        assertThat(process1).isEqualTo(process2);
        process2.setId("002");
        assertThat(process1).isNotEqualTo(process2);
        process1.setId(null);
        assertThat(process1).isNotEqualTo(process2);
    }
}
