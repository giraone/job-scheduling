package com.giraone.jobs.service.dto;

import com.giraone.jobs.domain.enumeration.ActivationEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link com.giraone.jobs.domain.Process} entity.
 */
@Schema(description = "Process (the job type).")
public class ProcessDTO implements Serializable {

    private Long id;

    /**
     * Alias/Key of process.
     */
    @NotNull
    @Schema(description = "Alias/Key of process.", required = true)
    private String key;

    /**
     * Name of process.
     */
    @NotNull
    @Schema(description = "Name of process.", required = true)
    private String name;

    /**
     * Is process active or paused?
     */
    @NotNull
    @Schema(description = "Is process active or paused?", required = true)
    private ActivationEnum activation;

    /**
     * Agent key on which the process is performed.
     */
    @Schema(description = "Agent key on which the process is performed.")
    private String agentKey;

    /**
     * Bucket key to be used, if process is paused.
     */
    @Schema(description = "Bucket key to be used, if process is paused.")
    private String bucketKeyIfPaused;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProcessDTO)) {
            return false;
        }

        ProcessDTO processDTO = (ProcessDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, processDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProcessDTO{" +
            "id=" + getId() +
            ", key='" + getKey() + "'" +
            ", name='" + getName() + "'" +
            ", activation='" + getActivation() + "'" +
            ", agentKey='" + getAgentKey() + "'" +
            ", bucketKeyIfPaused='" + getBucketKeyIfPaused() + "'" +
            "}";
    }
}
