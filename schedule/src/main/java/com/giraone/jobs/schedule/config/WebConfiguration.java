package com.giraone.jobs.schedule.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
// Take care with @EnableWebFlux - see https://github.com/spring-projects/spring-boot/issues/13277
@EnableWebFlux
@Slf4j
public class WebConfiguration implements WebFluxConfigurer {

    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(true, false);

    // Needs Spring Boot 2.1.* or newer - this enables X-Forwarded header support
    @Bean
    public ForwardedHeaderTransformer forwardedHeaderTransformer() {
        log.info("ForwardedHeaderTransformer initialized");
        return new ForwardedHeaderTransformer();
    }

    // Map / to /index.html
    // See https://stackoverflow.com/a/50324512
    @Bean
    public RouterFunction<ServerResponse> indexHtmlRouter(@Value("classpath:/static/index.html") final Resource indexHtml) {
        return route(GET("/"), request -> ok().contentType(MediaType.TEXT_HTML).bodyValue(indexHtml));
    }

    @Bean
    public RouterFunction<ServerResponse> staticFilesResourceRouter() {
        return RouterFunctions.resources("/**", new ClassPathResource("static/"));
    }

    @Bean
    public WebClient veraViewWebClient(ApplicationProperties applicationProperties) {

        return prepareDefaultWebclient(MediaType.APPLICATION_JSON_VALUE,
            "http",
            applicationProperties.getJobAdminHost())
            .build();
    }

    @SuppressWarnings("SameParameterValue")
    private static WebClient.Builder prepareDefaultWebclient(String acceptedMediaType, String scheme, String host) {

        ExchangeStrategies strategies = ExchangeStrategies
            .builder()
            .codecs(clientCodecConfigurer ->
                clientCodecConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper))
            ).build();

        WebClient.Builder builder = WebClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT, acceptedMediaType)
            .baseUrl(UriComponentsBuilder.newInstance().scheme(scheme).host(host).build().toString())
            .exchangeStrategies(strategies);

        return builder;
    }
}
