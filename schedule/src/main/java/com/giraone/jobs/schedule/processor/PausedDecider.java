package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.schedule.clients.JobAdminClient;
import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.model.ActivationEnum;
import com.giraone.jobs.schedule.model.ProcessDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PausedDecider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PausedDecider.class);

    private final ApplicationProperties applicationProperties;
    private final JobAdminClient jobAdminClient;

    private long lastDecisionTimeMillis = 0L;
    private final Map<String, Integer> lastDecision = new HashMap<>();

    public PausedDecider(ApplicationProperties applicationProperties, JobAdminClient jobAdminClient) {
        this.applicationProperties = applicationProperties;
        this.jobAdminClient = jobAdminClient;
    }

    @Scheduled(fixedRate = 30000)
    public void scheduleReload() throws InterruptedException {

        Map<String, Integer> pausedMap = new HashMap<>();
        int bucket = 1; // TODO
        jobAdminClient.getProcesses().subscribe(processDTO -> {
           pausedMap.put(processDTO.getId(), processDTO.getActivation().equals(ActivationEnum.PAUSED) ? bucket : 0);
        });
    }

    // 0 = not paused, > 0 index of bucket
    public int isProcessPaused(String processKey) {

        final long now = System.currentTimeMillis();
        Integer ret;
        if (lastDecisionTimeMillis + applicationProperties.getPausedDeciderQueryWaitSeconds() * 1000L < now) {
            ProcessDTO process = jobAdminClient.getProcess(processKey).block();
            boolean paused = ActivationEnum.PAUSED.equals(process != null ? process.getActivation() : ActivationEnum.PAUSED);
            LOGGER.info(">>> IS-PAUSED {}={}", process.getId(), paused);
            // TODO: Dynamic bucket
            ret = paused ? 1 : 0;
            lastDecision.put(processKey, ret);
            lastDecisionTimeMillis = now;
        } else {
            ret = lastDecision.get(processKey);
        }
        return ret;
    }
}
