package com.giraone.jobs.schedule.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class UtilsAndConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsAndConstants.class);
    public static long sleepTime = 0L;

    // Hide
    private UtilsAndConstants() {
    }

    public static void simulationModeSleep() {
        if (sleepTime > 0L) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOGGER.warn("simulationModeSleep: Thread.sleep interrupted!", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public static String convertStackTraceToString(Throwable throwable) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (IOException ioe) {
            LOGGER.error("Cannot write stack trace", ioe);
            return throwable.getMessage();
        }
    }
}
