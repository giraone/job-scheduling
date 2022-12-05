package com.giraone.jobs.materialize;

import com.giraone.jobs.materialize.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
@EnableR2dbcRepositories
public class EventToDatabaseApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventToDatabaseApplication.class);

    private final Environment env;

    public EventToDatabaseApplication(Environment env) {
        this.env = env;
    }

    /**
     * Initializes documents.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * <p>
     */
    @PostConstruct
    public void initApplication() {

        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains("dev") && activeProfiles.contains("prod")) {
            LOGGER.error("You have misconfigured your application! It should not run " +
                "with both the 'dev' and 'prod' profiles at the same time.");
        }
        if (activeProfiles.contains("dev") && activeProfiles.contains("cloud")) {
            LOGGER.error("You have misconfigured your application! It should not " +
                "run with both the 'dev' and 'cloud' profiles at the same time.");
        }
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EventToDatabaseApplication.class);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {

        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        if (!StringUtils.hasText(serverPort)) {
            serverPort = "8080";
        }
        String contextPath = env.getProperty("server.servlet.context-path");
        if (!StringUtils.hasText(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.warn("The host name could not be determined, using `localhost` as fallback");
        }
        LOGGER.info("\n----------------------------------------------------------\n" +
                "\t~~~ Application '{}' is running! Access URLs:\n" +
                "\t~~~ - Local:      {}://localhost:{}{}\n" +
                "\t~~~ - External:   {}://{}:{}{}\n" +
                "\t~~~ Java version:      {} / {}\n" +
                "\t~~~ Processors:        {}\n" +
                "\t~~~ Profile(s):        {}\n" +
                "\t~~~ Default charset:   {}\n" +
                "\t~~~ File encoding:     {}\n" +
                "----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            System.getProperty("java.version"), System.getProperty("java.vm.name"),
            Runtime.getRuntime().availableProcessors(),
            env.getActiveProfiles(),
            Charset.defaultCharset().displayName(),
            System.getProperty("file.encoding")
        );
    }
}