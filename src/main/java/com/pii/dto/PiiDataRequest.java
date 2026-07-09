package com.pii.dto;

import jakarta.validation.constraints.NotBlank;

public class PiiDataRequest {

    @NotBlank(message = "Data type is required")
    private String dataType;

    @NotBlank(message = "Value is required")
    private String value;

    @NotBlank(message = "Owner ID is required")
    private String ownerId;

    public PiiDataRequest() {}

    public PiiDataRequest(String dataType, String value, String ownerId) {
        this.dataType = dataType;
        this.value = value;
        this.ownerId = ownerId;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
