package com.pii.dto;

import jakarta.validation.constraints.NotBlank;

public record PiiDataRequest(
    @NotBlank(message = "PII data is required")
    String piiData,

    @NotBlank(message = "Request ID is required")
    String requestId
) {}
