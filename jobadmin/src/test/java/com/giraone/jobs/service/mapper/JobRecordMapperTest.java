package com.giraone.jobs.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobRecordMapperTest {

    private JobRecordMapper jobRecordMapper;

    @BeforeEach
    public void setUp() {
        jobRecordMapper = new JobRecordMapperImpl();
    }
}
