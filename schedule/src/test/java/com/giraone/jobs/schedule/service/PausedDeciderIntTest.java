package com.giraone.jobs.schedule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.jobs.common.ObjectMapperBuilder;
import com.giraone.jobs.schedule.config.ApplicationProperties;
import com.giraone.jobs.schedule.model.ActivationEnum;
import com.giraone.jobs.schedule.model.ProcessDTO;
import com.giraone.jobs.schedule.stopper.SwitchOnOff;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
    private SwitchOnOff switchOnOff;
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
    void loadProcesses() {

        // arrange
        configureTargetMockServer(false, true);

        // act
        List<ProcessDTO> result = pausedDecider.loadProcesses();

        // assert
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getKey()).isEqualTo("1");
        assertThat(result.get(0).getActivation()).isEqualTo(ActivationEnum.ACTIVE);
        assertThat(result.get(0).getAgentKey()).isEqualTo("A01");
        assertThat(result.get(0).getBucketKeyIfPaused()).isNull();

        assertThat(result.get(1).getKey()).isEqualTo("2");
        assertThat(result.get(1).getActivation()).isEqualTo(ActivationEnum.PAUSED);
        assertThat(result.get(1).getAgentKey()).isEqualTo("A02");
        assertThat(result.get(1).getBucketKeyIfPaused()).isEqualTo("B01");

        verifyMockServerGetRequest();
    }

    @ParameterizedTest
    @CsvSource({
        "false,true,,B01,true,true",
        "true,false,B01,,true,false",
    })
    void scheduleReload(boolean paused1, boolean paused2,
                        String expectedBucket1, String expectedBucket2,
                        boolean expectedB01Running /* always true */, boolean expectedB01Paused) {

        // arrange
        configureTargetMockServer(paused1, paused2);

        // act
        pausedDecider.scheduleReload();

        // assert
        verifyMockServerGetRequest();
        assertThat(pausedDecider.getBucketIfProcessPaused("1")).isEqualTo(expectedBucket1);
        assertThat(pausedDecider.getBucketIfProcessPaused("2")).isEqualTo(expectedBucket2);
        assertThat(switchOnOff.isRunningForProcessResume("B01")).isEqualTo(expectedB01Running);
        assertThat(switchOnOff.isPausedForProcessResume("B01")).isEqualTo(expectedB01Paused);
    }

    private void configureTargetMockServer(boolean paused1, boolean paused2) {

        ProcessDTO processDTO1 = new ProcessDTO();
        processDTO1.setKey("1");
        processDTO1.setAgentKey("A01");
        if (paused1) {
            processDTO1.setActivation(ActivationEnum.PAUSED);
            processDTO1.setBucketKeyIfPaused("B01");
        } else {
            processDTO1.setActivation(ActivationEnum.ACTIVE);
        }
        ProcessDTO processDTO2 = new ProcessDTO();
        processDTO2.setKey("2");
        processDTO2.setAgentKey("A02");
        processDTO2.setActivation(ActivationEnum.PAUSED);
        if (paused2) {
            processDTO2.setActivation(ActivationEnum.PAUSED);
            processDTO2.setBucketKeyIfPaused("B01");
        } else {
            processDTO2.setActivation(ActivationEnum.ACTIVE);
        }
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
