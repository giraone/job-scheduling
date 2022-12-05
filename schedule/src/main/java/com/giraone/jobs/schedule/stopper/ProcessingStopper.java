package com.giraone.jobs.schedule.stopper;

import java.util.Map;

public interface ProcessingStopper {

    void reset();

    boolean addErrorAndCheckStop();

    boolean addSuccessAndCheckResume();

    String dumpStatus();

    Map<String, Object> getStatus();
}
