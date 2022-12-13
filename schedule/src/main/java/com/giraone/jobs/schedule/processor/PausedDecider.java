package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.schedule.clients.JobAdminClient;
import com.giraone.jobs.schedule.config.ApplicationProperties;
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

    private Map<String, Integer> pausedMap = new HashMap<>();

    public PausedDecider(JobAdminClient jobAdminClient) {
        this.jobAdminClient = jobAdminClient;
    }

    @Scheduled(fixedRateString = "${application.loadProcessStatus.fixedRateMs}", initialDelayString = "${application.loadProcessStatus.initialDelayMs}")
    public void scheduleReload() {
        pausedMap = loadPausedMap();
    }

    // 0 = not paused, > 0 index of bucket
    public int isProcessPaused(String processKey) {

       return pausedMap.get(processKey);
    }

    protected Map<String, Integer> loadPausedMap() {

        Map<String, Integer> newPausedMap = new HashMap<>();
        int bucket = 1; // TODO
        jobAdminClient.getProcesses().doOnNext(processDTO -> {
            int zeroOrIndex = processDTO.getActivation().equals(ActivationEnum.PAUSED) ? bucket : 0;
            LOGGER.info(">>> IS-PAUSED {}={}", processDTO.getId(), zeroOrIndex);
            newPausedMap.put(processDTO.getId(), zeroOrIndex);
        }).blockLast();
        return newPausedMap;
    }
}
