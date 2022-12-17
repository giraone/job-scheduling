package com.giraone.jobs.materialize.model;

import java.io.Serializable;

public class DatabaseResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String entityId;
    private final boolean success;
    private final DatabaseOperation operation;

    public DatabaseResult(String entityId, boolean success, DatabaseOperation operation) {
        this.entityId = entityId;
        this.success = success;
        this.operation = operation;
    }

    public String getEntityId() {
        return entityId;
    }

    public boolean isSuccess() {
        return success;
    }

    public DatabaseOperation getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        return "DatabaseResult{" +
            "entityId=" + entityId +
            ", success=" + success +
            ", operation=" + operation +
            '}';
    }
}
