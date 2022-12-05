package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import com.giraone.jobs.schedule.model.StillPausedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessorResume {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorResume.class);

    private final PausedDecider pausedDecider;

    public ProcessorResume(PausedDecider pausedDecider) {
        this.pausedDecider = pausedDecider;
    }

    public JobScheduledEvent streamProcess(JobPausedEvent jobPausedEvent) {

        LOGGER.debug(">>> ProcessorResume.streamProcess {}", jobPausedEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobPausedEvent.getId() == 0L) {
            throw new IllegalArgumentException("Forced runtime exception because id is null");
        }

        String processKey = jobPausedEvent.getProcessKey();
        Integer pausedBucket = pausedDecider.isProcessPaused(processKey);
        if (pausedBucket != null) {
            throw new StillPausedException("Process " + processKey + " is still paused.", jobPausedEvent);
        }
        return new JobScheduledEvent(jobPausedEvent);
    }
}
