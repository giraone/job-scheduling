package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.AbstractJobEvent;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobFailedEvent;
import com.giraone.jobs.events.JobNotifiedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.exceptions.DocumentedErrorOutput;
import com.giraone.jobs.schedule.model.AgentFailedException;
import com.giraone.jobs.schedule.model.PausedException;
import com.giraone.jobs.schedule.model.StillPausedException;
import com.giraone.jobs.schedule.stopper.DefaultProcessingStopperImpl;
import com.giraone.jobs.schedule.stopper.ProcessingStopper;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.cloud.stream.endpoint.BindingsEndpoint;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    public static final Map<String, ProcessingStopper> STOPPER = Map.of(
        "processSchedule", new DefaultProcessingStopperImpl(),
        "processResume", new DefaultProcessingStopperImpl(),
        "processAgentA01", new DefaultProcessingStopperImpl(),
        "processAgentA02", new DefaultProcessingStopperImpl(),
        "processAgentA03", new DefaultProcessingStopperImpl(),
        "processNotify", new DefaultProcessingStopperImpl()
    );

    private final ApplicationProperties applicationProperties;
    private final BindingsEndpoint bindingsEndpoint;
    private final StreamBridge streamBridge;
    private final ProcessorSchedule processorSchedule;
    private final ProcessorResume processorResume;
    private final ProcessorAgent processorAgent;
    private final ProcessorNotify processorNotify;

    public EventProcessor(ApplicationProperties applicationProperties,
                          BindingsEndpoint bindingsEndpoint, StreamBridge streamBridge,
                          ProcessorSchedule processorSchedule,
                          ProcessorResume processorResume,
                          ProcessorAgent processorAgent,
                          ProcessorNotify processorNotify
    ) {

        this.applicationProperties = applicationProperties;
        this.bindingsEndpoint = bindingsEndpoint;
        this.streamBridge = streamBridge;
        this.processorSchedule = processorSchedule;
        this.processorResume = processorResume;
        this.processorAgent = processorAgent;
        this.processorNotify = processorNotify;
    }

    @Bean
    public Consumer<KStream<String, JobAcceptedEvent>> processSchedule() {

        final AtomicReference<KeyValue<String, JobScheduledEvent>> result = new AtomicReference<>(null);
        return input -> input
            .filter(
                (messageKey, messageValue) -> {
                    try { // Call the processor within try/catch
                        JobScheduledEvent event = processorSchedule.streamProcess(messageValue);
                        return handleDynamicAgentTarget("processSchedule", event);
                    } catch (PausedException exception) {
                        handleProcessingSuccess("processSchedule", exception.getMessageObject().getMessageKey());
                        return handlePausedException("processSchedule", exception.getMessageObject());
                    } catch (Exception exception) {
                        // We have sent the error and signal with false, that there is no output
                        return handleProcessingException("processSchedule", messageKey, messageValue, exception);
                    }
                })
            .map((messageKey, messageValue) -> result.get());
    }

    @Bean
    public Function<KStream<String, JobPausedEvent>, KStream<String, JobScheduledEvent>> processResumeB01() {
        return processResume("processResumeB01");
    }

    @Bean
    public Function<KStream<String, JobPausedEvent>, KStream<String, JobScheduledEvent>> processResumeB02() {
        return processResume("processResumeB02");
    }

    private Function<KStream<String, JobPausedEvent>, KStream<String, JobScheduledEvent>> processResume(String processName) {

        final AtomicReference<KeyValue<String, JobScheduledEvent>> result = new AtomicReference<>(null);
        return input -> input
            .filter(
                (messageKey, messageValue) -> {
                    try { // Call the processor within try/catch
                        JobScheduledEvent event = processorResume.streamProcess(messageValue);
                        return handleDynamicAgentTarget(processName, event);
                    } catch (StillPausedException exception) {
                        LOGGER.info(">>> STILL-PAUSED \"{}\"!", exception.getMessageObject().getProcessKey());
                        throw exception; // re-throw, then it is still blocked
                    } catch (Exception exception) {
                        // We have sent the error and signal with false, that there is no output
                        return handleProcessingException(processName, messageKey, messageValue, exception);
                    }
                })
            .map((messageKey, messageValue) -> result.get());
    }

    @Bean
    public Function<KStream<String, JobScheduledEvent>, KStream<String, JobCompletedEvent>> processAgentA01() {
        return processAgent("processAgentA01");
    }

    @Bean
    public Function<KStream<String, JobScheduledEvent>, KStream<String, JobCompletedEvent>> processAgentA02() {
        return processAgent("processAgentA02");
    }

    @Bean
    public Function<KStream<String, JobScheduledEvent>, KStream<String, JobCompletedEvent>> processAgentA03() {
        return processAgent("processAgentA03");
    }

    private Function<KStream<String, JobScheduledEvent>, KStream<String, JobCompletedEvent>> processAgent(String processName) {

        final AtomicReference<KeyValue<String, JobCompletedEvent>> result = new AtomicReference<>(null);
        return input -> input
            .filter(
                (messageKey, messageValue) -> {
                    try { // Call the processor within try/catch
                        result.set(processorAgent.streamProcess(messageKey, messageValue));
                        return handleProcessingSuccess(processName, messageKey);
                    } catch (AgentFailedException exception) {
                        return handleAgentFailedException(processName, exception.getMessageObject());
                    } catch (Exception exception) {
                        // We have sent the error and signal with false, that there is no output
                        return handleProcessingException(processName, messageKey, messageValue, exception);
                    }
                })
            .map((messageKey, messageValue) -> result.get());
    }

    @Bean
    public Function<KStream<String, JobCompletedEvent>, KStream<String, JobNotifiedEvent>> processNotify() {

        final AtomicReference<KeyValue<String, JobNotifiedEvent>> result = new AtomicReference<>(null);
        return input -> input
            .filter(
                (messageKey, messageValue) -> {
                    try { // Call the processor within try/catch
                        result.set(processorNotify.streamProcess(messageKey, messageValue));
                        // We are done and signal with true, that the flow has an output
                        return handleProcessingSuccess("processNotify", messageKey);
                    } catch (Exception exception) {
                        // We have sent the error and signal with false, that there is no output
                        return handleProcessingException("processNotify", messageKey, messageValue, exception);
                    }
                })
            .map((messageKey, messageValue) -> result.get());
    }

    //------------------------------------------------------------------------------------------------------------------

    protected boolean handleProcessingSuccess(String processName, String messageKey) {

        LOGGER.info(">>> SUCCESS in process {} for message={}.", processName, messageKey);
        if (!applicationProperties.isDisableStopper()) {
            final ProcessingStopper processingStopper = EventProcessor.STOPPER.get(processName);
            if (processingStopper != null) {
                processingStopper.addSuccessAndCheckResume();
            } else {
                LOGGER.error(">>> No process with name \"{}\"!", processName);
            }
        }
        return true;
    }

    protected boolean handleDynamicAgentTarget(String processName, JobScheduledEvent jobEvent) {

        final Message<JobScheduledEvent> message = MessageBuilder.withPayload(jobEvent)
            .setHeader(KafkaHeaders.MESSAGE_KEY, jobEvent.getMessageKey())
            .build();
        final String bindingName = processName + "-" + jobEvent.getAgentSuffix();
        boolean ok = streamBridge.send(bindingName, message);
        if (!ok) {
            LOGGER.error(">>> Cannot send paused event to out binding \"{}\"!", bindingName);
        }
        return false;
    }

    protected boolean handlePausedException(String processName, JobPausedEvent jobEvent) {

        final Message<JobPausedEvent> message = MessageBuilder.withPayload(jobEvent)
            .setHeader(KafkaHeaders.MESSAGE_KEY, jobEvent.getMessageKey())
            .build();
        final String bindingName = processName + "-" + jobEvent.getBucketSuffix();
        boolean ok = streamBridge.send(bindingName, message);
        if (!ok) {
            LOGGER.error(">>> Cannot send paused event to out binding \"{}\"!", bindingName);
        } else {
            LOGGER.info(">>> PAUSED \"{}\"!", jobEvent);
        }
        return false;
    }

    protected boolean handleAgentFailedException(String processName, JobFailedEvent jobEvent) {

        final Message<JobFailedEvent> message = MessageBuilder.withPayload(jobEvent)
            .setHeader(KafkaHeaders.MESSAGE_KEY, jobEvent.getMessageKey())
            .build();
        final String bindingName = processName + "-out-failed";
        boolean ok = streamBridge.send(bindingName, message);
        if (!ok) {
            LOGGER.error(">>> Cannot send paused event to out binding \"{}\"!", bindingName);
        } else {
            LOGGER.info(">>> FAILED \"{}\"!", jobEvent);
        }
        return false;
    }

    protected boolean handleProcessingException(String processName, String messageKey, Object messageValue, Exception exception) {

        final DocumentedErrorOutput documentedErrorOutput = new DocumentedErrorOutput(messageKey, messageValue, exception);
        final Message<DocumentedErrorOutput> documentedErrorOutputMessage = MessageBuilder
            .withPayload(documentedErrorOutput)
            .setHeader(KafkaHeaders.MESSAGE_KEY, messageKey).build();
        final String bindingNameError = processName + "-out-error";
        final String bindingNameConsumer = processName + "-in-0";
        LOGGER.error(">>> EXCEPTION in process {} for message={}! Sending problem to out binding \"{}\".",
            processName, messageValue, bindingNameError, exception);
        boolean ok = streamBridge.send(bindingNameError, documentedErrorOutputMessage);
        if (!ok) {
            LOGGER.error(">>> Cannot send problem to out binding \"{}\"!", bindingNameError);
        }

        if (!applicationProperties.isDisableStopper()) {
            final ProcessingStopper processingStopper = EventProcessor.STOPPER.get(processName);
            if (processingStopper != null) {
                boolean stop = processingStopper.addErrorAndCheckStop();
                if (stop) {
                    LOGGER.error(">>> STOPPING {} - - - STOPPING - - - STOPPING - - -", bindingNameConsumer);
                    Binding<?> state = bindingsEndpoint.queryState(bindingNameConsumer);
                    if (state.isRunning()) {
                        bindingsEndpoint.changeState(bindingNameConsumer, BindingsLifecycleController.State.STOPPED);
                    }
                }
            } else {
                LOGGER.error(">>> No process with name \"{}\"!", processName);
            }
        }

        return false;
    }

    // PARTITIONING

    @Bean
    public StreamPartitioner<String, AbstractJobEvent> streamPartitionerDefault() {

        LOGGER.info("Performing StreamPartitioner default setup using EventProcessor.streamPartitioner");
        return (topicName, key, value, totalPartitions) -> {
            final int partition = key != null ? Math.abs(key.hashCode()) % totalPartitions : 0;
            LOGGER.info(">>> streamPartitionerDefault topicName={} totalPartitions={} key={} partition={}", topicName, totalPartitions, key, partition);
            return partition;
        };
    }
}
