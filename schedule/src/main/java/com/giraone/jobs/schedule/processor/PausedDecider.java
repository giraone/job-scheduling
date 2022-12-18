package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.schedule.clients.JobAdminClient;
import com.giraone.jobs.schedule.model.ActivationEnum;
import com.giraone.jobs.schedule.model.ProcessDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
        pausedMap = loadPausedMap().block();
    }

    // null = not paused, != null key of bucket
    public String isProcessPaused(String processKey) {

        final String ret = pausedMap.get(processKey);
        if (ret != null) {
            LOGGER.info(">>> {} IS-PAUSED", processKey);
        }
        return ret;
    }

    protected Mono<Map<String, String>> loadPausedMap() {

        return jobAdminClient.getProcesses()
            .filter(processDTO -> processDTO.getActivation() == ActivationEnum.PAUSED)
            .doOnNext(processDTO -> {
                LOGGER.info(">>> {} IS-PAUSED with Paused-Bucket={} and Agent='{}'",
                    processDTO.getKey(), processDTO.getBucketKeyIfPaused(), processDTO.getAgentKey());
            })
            .collectMap(ProcessDTO::getKey, ProcessDTO::getBucketKeyIfPaused);
    }
}
