package com.giraone.jobs.receiver.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * The class is mainly a "template" for the controller/service set up and its tests.
 */
@Service
public class PingService {

    public Mono<String> getOkString() {
        return Mono.just("OK");
    }
}
