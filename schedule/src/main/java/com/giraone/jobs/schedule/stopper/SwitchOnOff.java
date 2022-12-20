package com.giraone.jobs.schedule.stopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.cloud.stream.endpoint.BindingsEndpoint;
import org.springframework.stereotype.Service;

@Service
public class SwitchOnOff {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchOnOff.class);

    private final BindingsEndpoint bindingsEndpoint;

    public SwitchOnOff(BindingsEndpoint bindingsEndpoint) {
        this.bindingsEndpoint = bindingsEndpoint;
    }

    public boolean changeStateToPaused(String processorName, boolean paused) {

        final String bindingNameConsumer = processorName + "-in-0";
        Binding<?> state = bindingsEndpoint.queryState(bindingNameConsumer);
        if (state == null) {
            throw new IllegalArgumentException("bindingNameConsumer \"" + bindingNameConsumer + "\" + wrong. No state!");
        }

        if (!state.isRunning()) {
                LOGGER.info("~~~ STARTING ~~~~~~~~ {}", bindingNameConsumer);
            bindingsEndpoint.changeState(bindingNameConsumer, BindingsLifecycleController.State.STARTED);
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        state = bindingsEndpoint.queryState(bindingNameConsumer);

        if (paused) {
            if (!state.isPaused()) {
                LOGGER.info("~~~ PAUSING ~~~~~~~~~ {}", bindingNameConsumer);
                bindingsEndpoint.changeState(bindingNameConsumer, BindingsLifecycleController.State.PAUSED);

            } else {
                LOGGER.warn("~~~ Attempt to PAUSE  {}, but is running={}, paused={}", bindingNameConsumer, state.isRunning(), state.isPaused());
            }
        } else {
            if (state.isPaused()) {
                LOGGER.info("~~~ RESUMING ~~~~~~~~ {}", bindingNameConsumer);
                bindingsEndpoint.changeState(bindingNameConsumer, BindingsLifecycleController.State.RESUMED);
            } else {
                LOGGER.warn("~~~ Attempt to RESUME {}, but is running={}, paused={}", bindingNameConsumer, state.isRunning(), state.isPaused());
            }
        }
        final Binding<?> newState = bindingsEndpoint.queryState(bindingNameConsumer);
                LOGGER.info("~~~ NEW-STATE ~~~~~~~ {}: running={}, paused={}", bindingNameConsumer, newState.isRunning(), newState.isPaused());
        return newState.isPaused();
    }

    public boolean isRunning(String processorName) {

        final String bindingNameConsumer = processorName + "-in-0";
        Binding<?> state = bindingsEndpoint.queryState(bindingNameConsumer);
        return state.isRunning();
    }

    public boolean isPaused(String processorName) {

        final String bindingNameConsumer = processorName + "-in-0";
        Binding<?> state = bindingsEndpoint.queryState(bindingNameConsumer);
        return state.isPaused();
    }

    //--- short cuts for processResume ---

    public boolean changeStateToPausedForProcessResume(String bucketPausedKey, boolean paused) {
        return changeStateToPaused("processResume" + bucketPausedKey, paused);
    }

    public boolean isRunningForProcessResume(String bucketPausedKey) {
        return isRunning("processResume" + bucketPausedKey);
    }

    public boolean isPausedForProcessResume(String bucketPausedKey) {
        return isPaused("processResume" + bucketPausedKey);
    }
}
