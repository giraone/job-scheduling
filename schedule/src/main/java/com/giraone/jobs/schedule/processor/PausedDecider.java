package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.schedule.clients.JobAdminClient;
import com.giraone.jobs.schedule.model.ActivationEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PausedDecider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PausedDecider.class);

    private final JobAdminClient jobAdminClient;

    private Map<String, String> pausedMap = new HashMap<>();

    public PausedDecider(JobAdminClient jobAdminClient) {
        this.jobAdminClient = jobAdminClient;
    }

    @Scheduled(fixedRateString = "${application.loadProcessStatus.fixedRateMs}", initialDelayString = "${application.loadProcessStatus.initialDelayMs}")
    public void scheduleReload() {
        pausedMap = loadPausedMap();
    }

    // null = not paused, != null key of bucket
    public String isProcessPaused(String processKey) {

        return pausedMap.get(processKey);
    }

    protected Map<String, String> loadPausedMap() {

        final Map<String, String> newPausedMap = new HashMap<>();
        jobAdminClient.getProcesses().doOnNext(processDTO -> {
            final String bucketKeyIfPaused = processDTO.getActivation() == ActivationEnum.PAUSED ? processDTO.getBucketKeyIfPaused() : null;
            LOGGER.info(">>> IS-PAUSED {} = {} {}", processDTO.getKey(), processDTO.getActivation(), bucketKeyIfPaused);
            if (bucketKeyIfPaused != null) {
                newPausedMap.put(processDTO.getKey(), bucketKeyIfPaused);
            }
        }).blockLast();
        return newPausedMap;
    }
}
