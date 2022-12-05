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

    private String id;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
            ", name='" + getName() + "'" +
            ", activation='" + getActivation() + "'" +
            "}";
    }
}
