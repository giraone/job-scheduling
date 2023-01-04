package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import com.giraone.jobs.events.AbstractAssignedJobEvent;
import com.giraone.jobs.events.AbstractJobEvent;
import com.giraone.jobs.events.AbstractJobStatusChangedEvent;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobFailedEvent;
import com.giraone.jobs.events.JobNotifiedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.exceptions.DocumentedErrorOutput;
import com.giraone.jobs.schedule.stopper.DefaultProcessingStopperImpl;
import com.giraone.jobs.schedule.stopper.ProcessingStopper;
import com.giraone.jobs.schedule.stopper.SwitchOnOff;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.cloud.stream.endpoint.BindingsEndpoint;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The central class where all processing logic starts. This class should not contain any
 * details of the processing (these are placed in the Processor* classes), but the technical aspects, e.g.
 * <ul>
 *     <li>Whether a Kafka or a Kafka Streams (KStream, KeyValue) binder is used.</li>
 *     <li>The central error handling.</li>
 * </ul>
 */
@Component
public class EventProcessor implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);
    private static final ObjectMapper mapper = ObjectMapperBuilder.build(false, false);

    private static final String METRICS_PREFIX = "schedule";
    private static final String METRICS_PROCESSING = ".processing.";
    private static final String METRICS_MESSAGES = ".messages.";
    private static final String METRICS_SUCCESS = ".success";
    private static final String METRICS_FAILURE = ".failure";

    public static final String PROCESS_schedule = "processSchedule";
    public static final String PROCESS_resume = "processResume";
    public static final String PROCESS_resume_B01 = PROCESS_resume + "B01";
    public static final String PROCESS_resume_B02 = PROCESS_resume + "B02";
    public static final String PROCESS_agent = "processAgent";
    public static final String PROCESS_agent_A01 = PROCESS_agent + "A01";
    public static final String PROCESS_agent_A02 = PROCESS_agent + "A02";
    public static final String PROCESS_agent_A03 = PROCESS_agent + "A03";
    public static final String PROCESS_notify = "processNotify";

    public static final Map<String, ProcessingStopper> STOPPER = Map.of(
        PROCESS_schedule, new DefaultProcessingStopperImpl(),
        PROCESS_resume_B01, new DefaultProcessingStopperImpl(),
        PROCESS_resume_B02, new DefaultProcessingStopperImpl(),
        PROCESS_agent_A01, new DefaultProcessingStopperImpl(),
        PROCESS_agent_A02, new DefaultProcessingStopperImpl(),
        PROCESS_agent_A03, new DefaultProcessingStopperImpl(),
        PROCESS_notify, new DefaultProcessingStopperImpl()
    );

    private final ApplicationProperties applicationProperties;
    private final MeterRegistry meterRegistry;
    private final SwitchOnOff switchOnOff;
    private final BindingsEndpoint bindingsEndpoint;
    private final StreamBridge streamBridge;
    private final ProcessorSchedule processorSchedule;
    private final ProcessorResume processorResume;
    private final ProcessorAgent processorAgent;
    private final ProcessorNotify processorNotify;

    private final Map<String, Counter> processSuccessCounter = new HashMap<>();
    private final Map<String, Counter> processFailureCounter = new HashMap<>();
    private final Map<String, Counter> topicMessageCounter = new HashMap<>();

    public EventProcessor(ApplicationProperties applicationProperties,
                          MeterRegistry meterRegistry,
                          SwitchOnOff switchOnOff,
                          BindingsEndpoint bindingsEndpoint,
                          StreamBridge streamBridge,
                          ProcessorSchedule processorSchedule,
                          ProcessorResume processorResume,
                          ProcessorAgent processorAgent,
                          ProcessorNotify processorNotify
    ) {
        this.applicationProperties = applicationProperties;
        this.meterRegistry = meterRegistry;
        this.switchOnOff = switchOnOff;
        this.bindingsEndpoint = bindingsEndpoint;
        this.streamBridge = streamBridge;
        this.processorSchedule = processorSchedule;
        this.processorResume = processorResume;
        this.processorAgent = processorAgent;
        this.processorNotify = processorNotify;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        this.switchOnOff.changeStateToPausedForProcessResume("B01", true);
        this.switchOnOff.changeStateToPausedForProcessResume("B02", true);
    }

    @PostConstruct
    private void init() {

        final String[] processorNames = applicationProperties.getProcessorNames().split(",");
        for (String processorName: processorNames) {
            initializeSuccessCounterForProcessor(processorName);
            initializeFailureCounterForProcessor(processorName);
        }
        final String[] topics = applicationProperties.getOutboundTopicList();
        for (String topic: topics) {
            if (topic != null) {
                initializeCounterForTopic(topic);
            }
        }
    }

    //- SCHEDULE -------------------------------------------------------------------------------------------------------

    @Bean
    public Consumer<byte[]> processSchedule() {
        return in ->
            tryCatchForConsumer(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobAcceptedEvent.class),
                (jobInput) -> performSchedule().accept(jobInput),
                PROCESS_schedule
            );
    }

    private Consumer<JobAcceptedEvent> performSchedule() {
        return jobAcceptedEvent -> {
            AbstractJobStatusChangedEvent event = processorSchedule.streamProcess(jobAcceptedEvent);
            sendToDynamicTarget(event, jobEvent -> {
                if (event instanceof final JobScheduledEvent jobScheduledEvent) {
                    // return PROCESS_schedule + "-" + jobScheduledEvent.getAgentKey();
                    return applicationProperties.getTopics().getTopicJobScheduled(jobScheduledEvent.getAgentKey());
                } else if (event instanceof final JobPausedEvent jobPausedEvent) {
                    // return PROCESS_schedule + "-" + jobPausedEvent.getPausedBucketKey();
                    return applicationProperties.getTopics().getTopicJobPaused(jobPausedEvent.getPausedBucketKey());
                } else {
                    throw new IllegalArgumentException("Event returned by processorSchedule has invalid type " + event.getClass());
                }
            });
        };
    }

    //- RESUME ---------------------------------------------------------------------------------------------------------

    @Bean
    public Consumer<byte[]> processResumeB01() {
        return in ->
            tryCatchForConsumer(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobPausedEvent.class),
                (jobInput) -> performResume(PROCESS_resume_B01).accept(jobInput),
                PROCESS_resume_B01
            );
    }

    @Bean
    public Consumer<byte[]> processResumeB02() {
        return in ->
            tryCatchForConsumer(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobPausedEvent.class),
                (jobInput) -> performResume(PROCESS_resume_B02).accept(jobInput),
                PROCESS_resume_B02
            );
    }

    private Consumer<JobPausedEvent> performResume(String processName) {
        return jobPausedEvent -> {
            Optional<JobScheduledEvent> jobScheduledEventOptional = processorResume.streamProcess(jobPausedEvent);
            if (jobScheduledEventOptional.isPresent()) {
                final JobScheduledEvent jobScheduledEvent = jobScheduledEventOptional.get();
                final String binding = applicationProperties.getTopics().getTopicJobScheduled(jobScheduledEvent.getAgentKey());
                sendToDynamicTarget(jobScheduledEvent, jobEvent -> binding);
            } else {
                LOGGER.warn(">>> STILL-PAUSED '{}'", processName);
                // TODO: Kein ACK
            }
        };
    }

    //- AGENT ----------------------------------------------------------------------------------------------------------

    @Bean
    public Consumer<byte[]> processAgentA01() {
        return in ->
            tryCatchForConsumer(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobScheduledEvent.class),
                (jobInput) -> performAgent(PROCESS_agent_A01).accept(jobInput),
                PROCESS_agent_A01
            );
    }

    @Bean
    public Consumer<byte[]> processAgentA02() {
        return in ->
            tryCatchForConsumer(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobScheduledEvent.class),
                (jobInput) -> performAgent(PROCESS_agent_A02).accept(jobInput),
                PROCESS_agent_A02
            );
    }

    @Bean
    public Consumer<byte[]> processAgentA03() {
        return in ->
            tryCatchForConsumer(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobScheduledEvent.class),
                (jobInput) -> performAgent(PROCESS_agent_A03).accept(jobInput),
                PROCESS_agent_A03
            );
    }

    private Consumer<JobScheduledEvent> performAgent(String processName) {
        return jobScheduledEvent -> {
            AbstractAssignedJobEvent event = processorAgent.streamProcess(jobScheduledEvent);
            sendToDynamicTarget(event, jobEvent -> {
                if (event instanceof JobCompletedEvent) {
                    LOGGER.info(">>> COMPLETED     {} of {} in agent '{}'", event.getMessageKey(), event.getProcessKey(), event.getAgentKey());
                    // return PROCESS_agent + event.getAgentKey() + "-out-0";
                    return applicationProperties.getTopics().getTopicJobCompleted();
                } else if (event instanceof JobFailedEvent) {
                    LOGGER.warn(">>> FAILED        {} of {} in agent '{}'", event.getMessageKey(), event.getProcessKey(), event.getAgentKey());
                    // return PROCESS_agent + event.getAgentKey() + "-out-failed";
                    return applicationProperties.getTopics().getTopicJobFailed(event.getAgentKey());
                } else {
                    throw new IllegalArgumentException("Event returned by processorAgent has invalid type " + event.getClass());
                }
            });
        };
    }

    //- NOTIFY ---------------------------------------------------------------------------------------------------------

    @Bean
    public Function<byte[], Message<byte[]>> processNotify() {
        return in ->
            tryCatchForProcessor(
                in,
                (messageBytesIn) -> deserialize(messageBytesIn, JobCompletedEvent.class),
                (jobInput) -> serializeWithMessageKey(performNotify().apply(jobInput)),
                "processNotify"
            );
    }

    private Function<JobCompletedEvent, JobNotifiedEvent> performNotify() {
        return processorNotify::streamProcess;
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Perform deserialization, processing, serialization, send with message header
     *
     * @param messageValue the message input as a byte array
     * @param deserializer the deserialization function resulting in an AbstractJobEvent
     * @param processor the processor with an AbstractJobEvent as the input and a Message as the output
     * @param processName the processor name for finding the corresponding error topic
     * @param <T> the concrete job class of the inbound job
     * @return an outbound job or null, if no outbound job should be sent (e.g. on error or when dynamic topics are used)
     */
    protected <T extends AbstractJobEvent> Message<byte[]> tryCatchForProcessor(
        byte[] messageValue,
        Function<byte[], T> deserializer,
        Function<T, Message<byte[]>> processor,
        String processName) {

        final T jobInput;
        try {
            jobInput = deserializer.apply(messageValue);
        } catch (Exception e) {
            handleProcessingException(processName, "error", messageValue, e);
            return null;
        }
        final String messageKey = jobInput.getMessageKey();
        final Message<byte[]> result;
        try {
            result = processor.apply(jobInput);
        } catch (Exception e) {
            handleProcessingException(processName, messageKey, messageValue, e);
            return null;
        }
        return result;
    }

    protected <T extends AbstractJobEvent> boolean tryCatchForConsumer(
        byte[] messageValue,
        Function<byte[], T> deserializer,
        Consumer<T> consumer,
        String processName) {

        final T jobInput;
        try {
            jobInput = deserializer.apply(messageValue);
        } catch (Exception e) {
            return handleProcessingException(processName, "error", messageValue, e);
        }
        final String messageKey = jobInput.getMessageKey();
        try {
            consumer.accept(jobInput);
        } catch (Exception e) {
            return handleProcessingException(processName, messageKey, messageValue, e);
        }
        return handleProcessingSuccess(processName, messageKey);
    }

    protected <T> T deserialize(byte[] messageInBody, Class<T> cls) {
        T messageIn;
        try {
            messageIn = mapper.readValue(messageInBody, cls);
        } catch (IOException e) {
            LOGGER.warn("Cannot deserialize {}: {}", new String(messageInBody), e.getMessage());
            throw new RuntimeException(e);
        }
        return messageIn;
    }

    protected byte[] serialize(AbstractJobEvent jobEvent) {
        final byte[] messageOutBody;
        try {
            messageOutBody = mapper.writeValueAsBytes(jobEvent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return messageOutBody;
    }

    protected Message<byte[]> serializeWithMessageKey(AbstractJobEvent jobEvent) {
        final byte[] messageOutBody = serialize(jobEvent);
        return MessageBuilder.withPayload(messageOutBody)
            .setHeader(KafkaHeaders.KEY, jobEvent.getMessageKey())
            .build();
    }

    protected boolean sendToDynamicTarget(AbstractJobEvent jobEvent, Function<AbstractJobEvent, String> dynamicTarget) {
        final String bindingName = dynamicTarget.apply(jobEvent);
        if (bindingName == null) {
            LOGGER.warn("No dynamic target for {}!", jobEvent);
            return false;
        }
        final Message<AbstractJobEvent> message = MessageBuilder.withPayload(jobEvent)
            .setHeader(KafkaHeaders.KEY, jobEvent.getMessageKey())
            .build();

        boolean ok = streamBridge.send(bindingName, message);
        if (!ok) {
            LOGGER.error("Cannot send event to out binding \"{}\"!", bindingName);
            return false;
        } else {
            increaseSentToBindingCounter(bindingName);
        }
        return true;
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    protected boolean handleProcessingSuccess(String processName, String messageKey) {

        LOGGER.debug("+++ SUCCESS in process {} for message={}.", processName, messageKey);
        if (!applicationProperties.isDisableStopper()) {
            final ProcessingStopper processingStopper = EventProcessor.STOPPER.get(processName);
            if (processingStopper != null) {
                processingStopper.addSuccessAndCheckResume();
            } else {
                LOGGER.error("No process with name \"{}\"!", processName);
            }
        }
        increaseProcessSuccessCounter(processName);
        return true;
    }

    protected boolean handleProcessingException(String processName, String messageKey, Object messageValue, Exception exception) {

        final DocumentedErrorOutput documentedErrorOutput = new DocumentedErrorOutput(messageKey, messageValue, exception);
        final Message<DocumentedErrorOutput> documentedErrorOutputMessage = MessageBuilder
            .withPayload(documentedErrorOutput)
            .setHeader(KafkaHeaders.KEY, messageKey).build();
        final String bindingNameError = processName + "-out-error";
        LOGGER.error("+++ EXCEPTION in process {} for message={}! Sending problem to out binding \"{}\".",
            processName, messageValue, bindingNameError, exception);
        boolean ok = streamBridge.send(bindingNameError, documentedErrorOutputMessage);
        if (!ok) {
            increaseProcessErrorCounter(processName);
            LOGGER.error("Cannot send problem to out binding \"{}\"!", bindingNameError);
        }

        if (!applicationProperties.isDisableStopper()) {
            final ProcessingStopper processingStopper = EventProcessor.STOPPER.get(processName);
            if (processingStopper != null) {
                boolean stop = processingStopper.addErrorAndCheckStop();
                if (stop) {
                    final String bindingNameConsumer = processName + "-in-0";
                    LOGGER.error("+++ STOPPING {} - - - STOPPING - - - STOPPING - - -", bindingNameConsumer);
                    Binding<?> state = bindingsEndpoint.queryState(bindingNameConsumer);
                    if (state.isRunning()) {
                        bindingsEndpoint.changeState(bindingNameConsumer, BindingsLifecycleController.State.STOPPED);
                    }
                }
            } else {
                LOGGER.error("+++ No process with name \"{}\"!", processName);
            }
        }
        increaseSentToBindingCounter(processName);
        return false;
    }

    private void increaseProcessSuccessCounter(String processName) {
        final Counter counter = processSuccessCounter.get(processName);
        if (counter != null) {
            counter.increment();
        } else {
            LOGGER.warn("No counter (processSuccessCounter) for {} defined!", processName);
        }
    }

    private void increaseProcessErrorCounter(String processName) {
        final Counter counter = processFailureCounter.get(processName);
        if (counter != null) {
            counter.increment();
        } else {
            LOGGER.warn("No counter (processFailureCounter) for {} defined!", processName);
        }
    }

    private void increaseSentToBindingCounter(String bindingName) {
        LOGGER.debug("SENT message TO destination TOPIC of binding {}", bindingName);
        final Counter counter = topicMessageCounter.get(bindingName);
        if (counter != null) {
            counter.increment();
        } else {
            LOGGER.warn("No counter (topicMessageCounter) for {} defined!", bindingName);
        }
    }

    private void initializeSuccessCounterForProcessor(String processorName) {
        final Counter counter = Counter.builder(METRICS_PREFIX + METRICS_PROCESSING + processorName + METRICS_SUCCESS)
            .description("Counter for all jobs, that are successfully processed by " + processorName  + ".")
            .register(meterRegistry);
        processSuccessCounter.put(processorName, counter);
        LOGGER.info("Initialized success counter for processor '{}'", processorName);
    }

    private void initializeFailureCounterForProcessor(String processorName) {
        final Counter counter = Counter.builder(METRICS_PREFIX + METRICS_PROCESSING + processorName + METRICS_FAILURE)
            .description("Counter for all jobs, that are NOT successfully processed by " + processorName  + ".")
            .register(meterRegistry);
        processFailureCounter.put(processorName, counter);
        LOGGER.info("Initialized failure counter for processor '{}'", processorName);
    }

    private void initializeCounterForTopic(String topic) {
        final Counter topicErrCounter = Counter.builder(METRICS_PREFIX + METRICS_MESSAGES + topic)
            .description("Counter for all jobs, that are NOT successfully SCHEDULED.")
            .register(meterRegistry);
        topicMessageCounter.put(topic, topicErrCounter);
        LOGGER.info("Initialized counter for topic '{}'", topic);
    }
}
