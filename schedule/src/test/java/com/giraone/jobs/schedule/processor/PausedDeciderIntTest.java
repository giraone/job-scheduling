package com.giraone.jobs.schedule.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.model.ActivationEnum;
import com.giraone.jobs.schedule.model.ProcessDTO;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(
    controlledShutdown = true,
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@SpringBootTest
@ActiveProfiles({"test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PausedDeciderIntTest {

    private static final int WIREMOCK_PORT = 8432;
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build(false, false);

    private final WireMockServer wireMockServer = new WireMockServer(WIREMOCK_PORT);

    @Autowired
    private PausedDecider pausedDecider;
    @Autowired
    private ApplicationProperties applicationProperties;

    @BeforeAll
    public void beforeAll() {
        this.wireMockServer.start();
    }

    @AfterAll
    public void afterAll() {
        this.wireMockServer.stop();
    }

    @AfterEach
    public void afterEach() {
        this.wireMockServer.resetAll();
    }

    @Test
    void loadPausedMap() {

        // arrange
        configureTargetMockServer();

        // act
        Map<String, String> result = pausedDecider.loadPausedMap().block();

        // assert
        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of( "2", "B01"));
        verifyMockServerGetRequest();
    }

    private void configureTargetMockServer() {

        ProcessDTO processDTO1 = new ProcessDTO();
        processDTO1.setKey("1");
        processDTO1.setAgentKey("A01");
        processDTO1.setActivation(ActivationEnum.ACTIVE);
        ProcessDTO processDTO2 = new ProcessDTO();
        processDTO2.setKey("2");
        processDTO2.setAgentKey("A02");
        processDTO2.setActivation(ActivationEnum.PAUSED);
        processDTO2.setBucketKeyIfPaused("B01");
        List<ProcessDTO> responseObject = List.of(
            processDTO1, processDTO2
        );
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(responseObject);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
        String resourcePath = applicationProperties.getJobAdminPathAll();
        ResponseDefinitionBuilder responseDefinitionBuilder = okForContentType(MediaType.APPLICATION_JSON_VALUE, jsonBody);
        MappingBuilder mappingBuilder = get(
            urlPathMatching(resourcePath) // we use path - not URL - matching!
        ).willReturn(responseDefinitionBuilder);
        wireMockServer.stubFor(mappingBuilder);
    }

    private void verifyMockServerGetRequest() {

        String resourcePath = applicationProperties.getJobAdminPathAll();
        RequestPatternBuilder requestPatternBuilderCheck = RequestPatternBuilder.newRequestPattern(
            RequestMethod.GET, urlPathMatching(resourcePath));
        // Check, that URL was called
        wireMockServer.verify(1, requestPatternBuilderCheck);
    }
}
