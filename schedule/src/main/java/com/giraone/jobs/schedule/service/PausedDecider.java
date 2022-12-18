package com.giraone.jobs.schedule.service;

import com.giraone.jobs.schedule.clients.JobAdminClient;
import com.giraone.jobs.schedule.model.ActivationEnum;
import com.giraone.jobs.schedule.model.ProcessDTO;
import com.giraone.jobs.schedule.stopper.SwitchOnOff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PausedDecider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PausedDecider.class);

    private final SwitchOnOff switchOnOff;
    private final JobAdminClient jobAdminClient;

    private Map<String, String> pausedMap = new HashMap<>();

    public PausedDecider(SwitchOnOff switchOnOff, JobAdminClient jobAdminClient) {
        this.switchOnOff = switchOnOff;
        this.jobAdminClient = jobAdminClient;
    }

    @Scheduled(fixedRateString = "${application.loadProcessStatus.fixedRateMs}", initialDelayString = "${application.loadProcessStatus.initialDelayMs}")
    public void scheduleReload() {

        final Map<String, String> newPausedMap = new HashMap<>();

        final List<ProcessDTO> newProcesses = loadProcesses();
        for (ProcessDTO process: newProcesses) {
            final String processKey = process.getKey();
            // (1) Build the map of process keys to paused bucket keys
            if (process.getActivation() == ActivationEnum.PAUSED) {
                LOGGER.info(">>> {} IS-PAUSED with Paused-Bucket={} and Agent='{}'",
                    processKey, process.getBucketKeyIfPaused(), process.getAgentKey());
                newPausedMap.put(processKey, process.getBucketKeyIfPaused());
                // Are there any processes to be switched from ACTIVE to PAUSED?
                if (!pausedMap.containsKey(processKey)) {
                    final String bucketKey = process.getBucketKeyIfPaused();
                    LOGGER.info(">>> SWITCHING {} from ACTIVE to PAUSED with bucket '{}'", processKey, bucketKey);
                    boolean ok = switchOnOff.changeStateToPausedForProcessResume(bucketKey, true);
                    if (!ok) {
                        LOGGER.error(">>> SWITCHING {} from ACTIVE to PAUSED with bucket '{}' FAILED!", processKey, bucketKey);
                    }
                }
            }
        }

        for (Map.Entry<String, String> kv: pausedMap.entrySet()) {
            final String processKey = kv.getKey();
            // Are there any processes to be switched from PAUSED to ACTIVE?
            if (!newPausedMap.containsKey(processKey)) {
                final String bucketKey = kv.getValue();
                LOGGER.info(">>> SWITCHING {} from PAUSED with bucket '{}' to ACTIVE", processKey,bucketKey);
                boolean ok = switchOnOff.changeStateToPausedForProcessResume(bucketKey, false);
                if (!ok) {
                    LOGGER.error(">>> SWITCHING {} from PAUSED with bucket '{}' to ACTIVE", processKey,bucketKey);
                }
            }
        }

        pausedMap = newPausedMap;
    }

    // null = not paused, != null key of bucket
    public String getBucketIfProcessPaused(String processKey) {

        final String ret = pausedMap.get(processKey);
        if (ret != null) {
            LOGGER.info(">>> {} IS-PAUSED", processKey);
        }
        return ret;
    }

    protected List<ProcessDTO> loadProcesses() {

        return jobAdminClient.getProcesses().collectList().block();
    }
}
