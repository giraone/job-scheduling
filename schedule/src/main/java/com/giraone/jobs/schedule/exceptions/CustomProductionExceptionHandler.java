package com.giraone.jobs.schedule.exceptions;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This exception handler is only called, when trying to interact with a broker
 * such as attempting to produce a record that is too large.
 */
@SuppressWarnings("unused")
public class CustomProductionExceptionHandler implements ProductionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomProductionExceptionHandler.class);

    @Override
    public ProductionExceptionHandlerResponse handle(ProducerRecord<byte[], byte[]> record, Exception exception) {

        final String keyAsString = record.key() != null ? new String(record.key()) : "";
        final String recordAsString = record.value() != null ? new String(record.value()) : "";
        LOGGER.error(">>> PRODUCER EXCEPTION key={}, value={}", keyAsString, recordAsString, exception);
        return ProductionExceptionHandlerResponse.FAIL;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // ignore
        LOGGER.debug("CustomProductionExceptionHandler.configure {}", configs.get("application.id"));
    }
}
