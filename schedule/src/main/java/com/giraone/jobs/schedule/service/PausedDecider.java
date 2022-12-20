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

/**
 * This class has two responsibilities:
 * <ul>
 *     <li>Decide, whether a process is paused and return the bucket key of the paused topic to be used.</li>
 *     <li>Map processes to agents and therefore to scheduled topics.</li>
 * </ul>
 * Both is done by asking the jobadmin service periodically and hold a cache for this.
 */
@Service
public class PausedDecider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PausedDecider.class);

    private final SwitchOnOff switchOnOff;
    private final JobAdminClient jobAdminClient;

    private Map<String, String> pausedMap = new HashMap<>();
    private Map<String, String> agentMap = new HashMap<>();

    public PausedDecider(SwitchOnOff switchOnOff, JobAdminClient jobAdminClient) {
        this.switchOnOff = switchOnOff;
        this.jobAdminClient = jobAdminClient;
    }

    @Scheduled(fixedRateString = "${application.loadProcessStatus.fixedRateMs}", initialDelayString = "${application.loadProcessStatus.initialDelayMs}")
    public void scheduleReload() {

        final Map<String, String> newPausedMap = new HashMap<>();
        final Map<String, String> newAgentMap = new HashMap<>();

        final List<ProcessDTO> newProcesses = loadProcesses();
        for (ProcessDTO process: newProcesses) {
            final String processKey = process.getKey();
            // (1) Build the map of process keys to paused bucket keys
            if (process.getActivation() == ActivationEnum.PAUSED) {
                LOGGER.info("<-> {} IS-PAUSED with bucket='{}' and agent='{}'",
                    processKey, process.getBucketKeyIfPaused(), process.getAgentKey());
                newPausedMap.put(processKey, process.getBucketKeyIfPaused());
                // (2) Are there any processes to be switched from ACTIVE to PAUSED?
                if (!pausedMap.containsKey(processKey)) {
                    final String bucketKey = process.getBucketKeyIfPaused();
                    LOGGER.info("<-> SWITCHING {} from ACTIVE to PAUSED with bucket='{}'", processKey, bucketKey);
                    boolean ok = switchOnOff.changeStateToPausedForProcessResume(bucketKey, true);
                    if (!ok) {
                        LOGGER.error("<-> SWITCHING {} from ACTIVE to PAUSED with bucket='{}' FAILED!", processKey, bucketKey);
                    }
                }
            }
            // (3) Build the map of process keys to agent keys
            newAgentMap.put(processKey, process.getAgentKey());
        }

        for (Map.Entry<String, String> kv: pausedMap.entrySet()) {
            final String processKey = kv.getKey();
            // (4) Are there any processes to be switched from PAUSED to ACTIVE?
            if (!newPausedMap.containsKey(processKey)) {
                final String bucketKey = kv.getValue();
                LOGGER.info("<-> SWITCHING {} from PAUSED with bucket '{}' to ACTIVE", processKey,bucketKey);
                boolean ok = switchOnOff.changeStateToPausedForProcessResume(bucketKey, false);
                if (!ok) {
                    LOGGER.error("<-> SWITCHING {} from PAUSED with bucket '{}' to ACTIVE", processKey,bucketKey);
                }
            }
        }

        pausedMap = newPausedMap;
        agentMap = newAgentMap;
    }

    // null = not paused, != null key of bucket
    public String getBucketIfProcessPaused(String processKey) {

        return pausedMap.get(processKey);
    }

    public String getAgentKeyForProcess(String processKey) {

        final String ret = agentMap.get(processKey);
        if (ret == null) {
            LOGGER.error("No agent found for processKey='{}'", processKey);
        }
        return ret;
    }

    protected List<ProcessDTO> loadProcesses() {

        return jobAdminClient.getProcesses().collectList().block();
    }
}
