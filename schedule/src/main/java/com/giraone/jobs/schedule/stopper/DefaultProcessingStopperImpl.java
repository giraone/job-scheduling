package com.giraone.jobs.schedule.stopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default Stopper Implementation. Stops, when ...
 * <ul>
 *     <li>there are more than n subsequent errors</li>
 *     <li>there are more than m error within one minute</li>
 * </ul>
 */
public class DefaultProcessingStopperImpl implements ProcessingStopper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProcessingStopperImpl.class);

    private int maxNumberOfSubsequentErrors = 3;
    private int maxNumberOfErrorsPerPeriod = 6;
    // Default period is one minute
    private long millisecondsOfPeriod = 60_000L;

    // total
    private final AtomicInteger numberOfErrorTotal = new AtomicInteger(0);
    private final AtomicInteger numberOfSuccessTotal = new AtomicInteger(0);
    // subsequent
    private final AtomicInteger numberOfSubsequentErrors = new AtomicInteger(0);
    // per period
    private final AtomicLong lastPeriod = new AtomicLong(System.currentTimeMillis() / millisecondsOfPeriod);
    private final AtomicInteger numberOfErrorInLastPeriod = new AtomicInteger(0);
    private final AtomicInteger numberOfSuccessInLastPeriod = new AtomicInteger(0);

    @Override
    public void reset() {

        // total
        numberOfErrorTotal.set(0);
        numberOfSuccessTotal.set(0);
        // subsequent
        numberOfSubsequentErrors.set(0);
        // per period
        lastPeriod.set(System.currentTimeMillis() / millisecondsOfPeriod);
        numberOfErrorInLastPeriod.set(0);
        numberOfSuccessInLastPeriod.set(0);
    }

    @Override
    public boolean addErrorAndCheckStop() {

        // total
        numberOfErrorTotal.incrementAndGet();
        // subsequent
        int n = numberOfSubsequentErrors.incrementAndGet();
        numberOfErrorInLastPeriod.incrementAndGet();
        if (n > maxNumberOfSubsequentErrors) {
            LOGGER.warn("### STOPPED after {} subsequent errors!", n);
            return true;
        }
        // per period
        long period = System.currentTimeMillis() / millisecondsOfPeriod;
        if (period == lastPeriod.get()) {
            int m = numberOfErrorInLastPeriod.incrementAndGet();
            if (m > maxNumberOfErrorsPerPeriod) {
                LOGGER.warn("### STOPPED after {} errors in last minute!", m);
                return true;
            }
        } else {
            numberOfErrorInLastPeriod.set(1);
        }

        return false;
    }

    @Override
    public boolean addSuccessAndCheckResume() {

        // total
        numberOfSuccessTotal.incrementAndGet();
        // subsequent
        numberOfSubsequentErrors.set(0);
        // per period
        long period = System.currentTimeMillis() / millisecondsOfPeriod;
        if (period == lastPeriod.get()) {
            numberOfSuccessInLastPeriod.incrementAndGet();
        } else {
            numberOfSuccessInLastPeriod.set(1);
            lastPeriod.set(period);
        }
        return false;
    }

    @Override
    public String dumpStatus() {
        return "DefaultProcessingStopperImpl.status={" +
            "errorTotal=" + numberOfErrorTotal +
            ", successTotal=" + numberOfSuccessTotal +
            ", subsequentErrors=" + numberOfSubsequentErrors +
            ", errorInLastPeriod=" + numberOfErrorInLastPeriod +
            ", successInLastPeriod=" + numberOfSuccessInLastPeriod +
            '}';
    }

    @Override
    public Map<String, Object> getStatus() {
        return Map.of(
            "success_total", numberOfSuccessTotal.get(),
            "error_total", numberOfErrorTotal.get()
        );
    }

    public void setMillisecondsOfPeriod(long millisecondsOfPeriod) {
        this.millisecondsOfPeriod = millisecondsOfPeriod;
    }

    @SuppressWarnings("unused")
    public void setMaxNumberOfSubsequentErrors(int maxNumberOfSubsequentErrors) {
        this.maxNumberOfSubsequentErrors = maxNumberOfSubsequentErrors;
    }

    @SuppressWarnings("unused")
    public void setMaxNumberOfErrorsPerPeriod(int maxNumberOfErrorsPerPeriod) {
        this.maxNumberOfErrorsPerPeriod = maxNumberOfErrorsPerPeriod;
    }

    public int getNumberOfErrorTotal() {
        return numberOfErrorTotal.get();
    }

    public int getNumberOfSuccessTotal() {
        return numberOfSuccessTotal.get();
    }

    @SuppressWarnings("unused")
    public int getNumberOfSubsequentErrors() {
        return numberOfSubsequentErrors.get();
    }

    @SuppressWarnings("unused")
    public int getNumberOfErrorInLastPeriod() {
        return numberOfErrorInLastPeriod.get();
    }

    @SuppressWarnings("unused")
    public int getNumberOfSuccessInLastPeriod() {
        return numberOfSuccessInLastPeriod.get();
    }
}
