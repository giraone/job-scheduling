package com.giraone.jobs.schedule.clients;

import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.model.ProcessDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class JobAdminClient extends AbstractClient {

    private final WebClient jobAdminWebClient;

    @Autowired
    public JobAdminClient(ApplicationProperties applicationProperties, WebClient jobAdminWebClient) {
        super(applicationProperties);
        this.jobAdminWebClient = jobAdminWebClient;
    }

    public Flux<ProcessDTO> getProcesses() {

        return this.jobAdminWebClient.get()
            .uri(uriBuilder -> uriBuilder.path(applicationProperties.getJobAdminPathAll()).build())
            .retrieve()
            .bodyToFlux(ProcessDTO.class)
            .doOnError(err ->
                log.error("SUBSYSTEM CALL-ERROR [jobAdmin]", err)
            );
    }
}

