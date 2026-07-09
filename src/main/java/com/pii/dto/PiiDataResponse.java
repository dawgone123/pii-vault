package com.pii.dto;

import java.time.LocalDateTime;

public class PiiDataResponse {

    private Long id;
    private String dataType;
    private String ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PiiDataResponse() {}

    public PiiDataResponse(Long id, String dataType, String ownerId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.dataType = dataType;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
