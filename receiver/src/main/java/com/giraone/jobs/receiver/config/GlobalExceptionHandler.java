package com.giraone.jobs.receiver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // e.g. org.apache.kafka.common.errors.TimeoutException
    @ExceptionHandler(org.apache.kafka.common.errors.RetriableException.class)
    ResponseEntity<String> postNotFound(org.apache.kafka.common.errors.RetriableException exception) {
        LOGGER.debug("Handling Kafka RetriableException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Kafka error. Retry later.");
    }
}
