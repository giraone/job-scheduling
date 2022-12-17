package com.giraone.jobs.schedule.model;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class ProcessDTO implements Serializable {

    @NotNull
    private String key;
    @NotNull
    private ActivationEnum activation;
    private String agentKey;
    private String bucketKeyIfPaused;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ActivationEnum getActivation() {
        return activation;
    }

    public void setActivation(ActivationEnum activation) {
        this.activation = activation;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public void setAgentKey(String agentKey) {
        this.agentKey = agentKey;
    }

    public String getBucketKeyIfPaused() {
        return bucketKeyIfPaused;
    }

    public void setBucketKeyIfPaused(String bucketKeyIfPaused) {
        this.bucketKeyIfPaused = bucketKeyIfPaused;
    }

    @Override
    public String toString() {
        return "ProcessDTO{" +
            "key='" + key + '\'' +
            ", activation=" + activation +
            ", agentKey='" + agentKey + '\'' +
            ", bucketKeyIfPaused='" + bucketKeyIfPaused + '\'' +
            '}';
    }
}
