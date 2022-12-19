package com.giraone.jobs.domain;

import com.giraone.jobs.domain.enumeration.ActivationEnum;

import java.io.Serial;
import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * Process (the job type).
 */
@Entity
@Table(name = "process")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Process implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * Alias/Key of process.
     */
    @NotNull
    @Column(name = "process_key", nullable = false)
    private String key;

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

    /**
     * Agent key on which the process is performed.
     */
    @Column(name = "agent_key")
    private String agentKey;

    /**
     * Bucket key to be used, if process is paused.
     */
    @Column(name = "bucket_key_if_paused")
    private String bucketKeyIfPaused;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Process id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return this.key;
    }

    public Process key(String key) {
        this.setKey(key);
        return this;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getAgentKey() {
        return this.agentKey;
    }

    public Process agentKey(String agentKey) {
        this.setAgentKey(agentKey);
        return this;
    }

    public void setAgentKey(String agentKey) {
        this.agentKey = agentKey;
    }

    public String getBucketKeyIfPaused() {
        return this.bucketKeyIfPaused;
    }

    public Process bucketKeyIfPaused(String bucketKeyIfPaused) {
        this.setBucketKeyIfPaused(bucketKeyIfPaused);
        return this;
    }

    public void setBucketKeyIfPaused(String bucketKeyIfPaused) {
        // ADAPTED
        this.bucketKeyIfPaused = "".equals(bucketKeyIfPaused) ? null : bucketKeyIfPaused;
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
            ", key='" + getKey() + "'" +
            ", name='" + getName() + "'" +
            ", activation='" + getActivation() + "'" +
            ", agentKey='" + getAgentKey() + "'" +
            ", bucketKeyIfPaused='" + getBucketKeyIfPaused() + "'" +
            "}";
    }
}
