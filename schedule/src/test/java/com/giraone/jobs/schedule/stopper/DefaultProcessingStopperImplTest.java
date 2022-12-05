package com.giraone.jobs.schedule.stopper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProcessingStopperImplTest {

    @ParameterizedTest
    // 1=success, 0=failure, expectedResult=true means was stopped
    @CsvSource({
        // subsequent tests
        "1:1:1,100,100,3,false,3,0",
        "0:0:0,100,100,3,false,0,3",
        "0:0:0:0,100,100,4,true,0,4",
        "1:0:1:0:0:0:1:1:1:1,100,100,10,false,6,4",
        "1:0:1:0:0:0:0:1:1:1,100,100,7,true,2,5",
        // per period tests
        "0:1:0:1:0:1:0:1:0:1,100,0,7,true,3,4"
    })
    void addErrorAndCheckStop(String sequenceString,
                              long periodInMilliseconds, long sleepMillisecondsAfterEachStep,
                              int expectedSteps, boolean expectedResult,
                              int expectedTotalNumberOfSuccess, int expectedTotalNumberOfFailures)
        throws InterruptedException {

        // arrange
        DefaultProcessingStopperImpl stopper = new DefaultProcessingStopperImpl();
        stopper.setMillisecondsOfPeriod(periodInMilliseconds);
        String[] sequenceArray = sequenceString.split(":");
        int done = 0;
        boolean result = true;

        // act
        for (String outcome : sequenceArray) {
            if ("1".equals(outcome)) {
                result = stopper.addSuccessAndCheckResume();
            } else {
                result = stopper.addErrorAndCheckStop();
            }
            System.out.println(stopper.dumpStatus());
            done++;
            if (result) {
                break;
            }
            Thread.sleep(sleepMillisecondsAfterEachStep);
        }

        // assert
        assertThat(done).isEqualTo(expectedSteps);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(stopper.getNumberOfSuccessTotal()).isEqualTo(expectedTotalNumberOfSuccess);
        assertThat(stopper.getNumberOfErrorTotal()).isEqualTo(expectedTotalNumberOfFailures);
    }

    @Test
    void checkStatusWorks() {

        // arrange
        DefaultProcessingStopperImpl stopper = new DefaultProcessingStopperImpl();

        // act
        stopper.addSuccessAndCheckResume();
        stopper.addErrorAndCheckStop();
        stopper.addSuccessAndCheckResume();

        // assert
        assertThat(stopper.dumpStatus()).contains("errorTotal=1");
        assertThat(stopper.getStatus()).isNotNull();
        assertThat(stopper.getStatus().get("success_total")).isEqualTo(2);
        assertThat(stopper.getStatus().get("error_total")).isEqualTo(1);
    }
}