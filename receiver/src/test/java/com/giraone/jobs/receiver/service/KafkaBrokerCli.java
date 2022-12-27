package com.giraone.jobs.receiver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.KafkaContainer;

public class KafkaBrokerCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBrokerCli.class);

    private KafkaContainer kafka;

    public KafkaBrokerCli(KafkaContainer kafka) {
        this.kafka = kafka;
    }

    public boolean createTopic(String topic) {

        // the broker is not "kafka.getBootstrapServers()" because it runs within the container - not outside from the host
        return runCmd("kafka-topics",
            "--bootstrap-server", "localhost:9092",
            "--create",
            "--topic", topic,
            "--replication-factor", "1",
            "--partitions", "2");
    }

    public String listTopics() {

        StringBuilder sb = new StringBuilder();
        if (runCmd(sb, "kafka-topics",
            "--bootstrap-server", "localhost:9092",
            "--list")) {
            return sb.toString();
        } else {
            return null;
        }
    }

    public String readTopic(String topic, int timeOutMs) {

        StringBuilder sb = new StringBuilder();
        if (runCmd(sb, "kafka-console-consumer",
            "--bootstrap-server", "localhost:9092",
            "--property", "print.key=true",
            "--topic", topic,
            "--timeout-ms", Integer.toString(timeOutMs),
            "--from-beginning")) {
            return sb.toString();
        } else {
            return null;
        }
    }

    public boolean runCmd(String... cmd) {

        return runCmd(null, cmd);
    }

    public boolean runCmd(StringBuilder sb, String... cmd) {

        LOGGER.info("EXECUTING in Kafka broker: {}", String.join(" ", cmd));
        Container.ExecResult result;
        try {
            result = kafka.execInContainer(cmd);
        } catch (Exception e) {
            LOGGER.error("Cannot execute {}", cmd, e);
            return false;
        }

        boolean ok = result.getExitCode() == 0;
        if (!ok) {
            LOGGER.error("ExecResult={}", result.getExitCode());
            LOGGER.error("{}", result.getStderr());
            LOGGER.error("{}", result.getStdout());
        } else if (sb != null) {
            sb.append(result.getStdout());
        }
        return ok;
    }
}
