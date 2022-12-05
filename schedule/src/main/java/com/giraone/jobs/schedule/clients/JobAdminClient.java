package com.giraone.jobs.schedule.clients;

import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.model.ProcessDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@Slf4j
@Service
public class JobAdminClient extends AbstractClient {

    private final WebClient jobAdminWebClient;

    @Autowired
    public JobAdminClient(ApplicationProperties applicationProperties, WebClient jobAdminWebClient) {
        super(applicationProperties);
        this.jobAdminWebClient = jobAdminWebClient;
    }

    public Mono<ProcessDTO> getProcess(String id) {

        return this.jobAdminWebClient.get()
            .uri(uriBuilder -> buildUri(uriBuilder, id))
            .retrieve()
            .bodyToMono(ProcessDTO.class)
            .doOnError(err ->
                log.error("SUBSYSTEM CALL-ERROR [jobAdmin]: id={}", id, err)
            );
    }

    private URI buildUri(UriBuilder uriBuilder, String id) {

        final String urlTemplate = applicationProperties.getJobAdminPath();
        Map<String, Object> params = Map.of("id", id);
        final URI ret = uriBuilder
            .path(urlTemplate)
            .build(params);
        log.debug("SUBSYSTEM URL [jobAdmin] = {}", ret);
        return ret;
    }
}

