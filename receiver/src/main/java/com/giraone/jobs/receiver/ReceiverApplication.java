package com.giraone.jobs.receiver;

import com.giraone.jobs.receiver.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class ReceiverApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverApplication.class);

    /**
     * Initializes documents.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * <p>
     */
    @PostConstruct
    public void initApplication() {

        final String nodeIndexString = System.getenv("CF_INSTANCE_INDEX");
        if (nodeIndexString != null) {
            LOGGER.info("Setting TSID node (tsidcreator.node) from CF_INSTANCE_INDEX to {}.", nodeIndexString);
            System.setProperty("tsidcreator.node", nodeIndexString);
        }
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ReceiverApplication.class);
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
        LOGGER.info("""
                ----------------------------------------------------------
                \t~~~ Application '{}' is running! Access URLs:
                \t~~~ - Local:      {}://localhost:{}{}
                \t~~~ - External:   {}://{}:{}{}
                \t~~~ Java version:      {} / {}
                \t~~~ Processors:        {}
                \t~~~ Profile(s):        {}
                \t~~~ Default charset:   {}
                \t~~~ File encoding:     {}
                ----------------------------------------------------------""",
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