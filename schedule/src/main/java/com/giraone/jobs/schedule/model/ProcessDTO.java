package com.giraone.jobs.schedule.model;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

public class ProcessDTO implements Serializable {

    private String id;

    /**
     * Name of process.
     */
    @NotNull
    private String name;

    /**
     * Is process active or paused?
     */
    @NotNull
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
