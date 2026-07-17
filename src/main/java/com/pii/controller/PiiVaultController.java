package com.pii.controller;

import com.pii.dto.PiiDataRequest;
import com.pii.dto.PiiDataResponse;
import com.pii.service.EncryptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PiiVaultController {

    private final EncryptionService encryptionService;

    public PiiVaultController(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @PostMapping("/encrypt")
    public ResponseEntity<PiiDataResponse> encryptPii(@Valid @RequestBody PiiDataRequest request) {
        String encryptedData = encryptionService.encrypt(request.piiData());
        UUID tokenId = UUID.nameUUIDFromBytes(encryptedData.getBytes());

        PiiDataResponse response = new PiiDataResponse(
            tokenId,
            encryptedData,
            Instant.now(),
            request.requestId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/decrypt")
    public ResponseEntity<String> decryptPii(@Valid @RequestBody PiiDataRequest request) {
        String decryptedData = encryptionService.decrypt(request.piiData());
        return ResponseEntity.ok(decryptedData);
    }
}
