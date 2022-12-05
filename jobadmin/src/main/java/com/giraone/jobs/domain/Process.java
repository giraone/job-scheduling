package com.giraone.jobs.domain;

import com.giraone.jobs.domain.enumeration.ActivationEnum;
import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * Process (the job type).
 */
@Entity
@Table(name = "process")
public class Process implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Alias/Key of process.
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * Name of process.
     */
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Is process active or paused?
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "activation", nullable = false)
    private ActivationEnum activation;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public String getId() {
        return this.id;
    }

    public Process id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Process name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ActivationEnum getActivation() {
        return this.activation;
    }

    public Process activation(ActivationEnum activation) {
        this.setActivation(activation);
        return this;
    }

    public void setActivation(ActivationEnum activation) {
        this.activation = activation;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Process)) {
            return false;
        }
        return id != null && id.equals(((Process) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Process{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", activation='" + getActivation() + "'" +
            "}";
    }
}
