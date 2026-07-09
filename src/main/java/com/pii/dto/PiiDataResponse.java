package com.pii.dto;

import java.time.Instant;
import java.util.UUID;

public record PiiDataResponse(
    UUID tokenId,
    String piiData,
    Instant encryptedAt,
    String requestId
) {}
