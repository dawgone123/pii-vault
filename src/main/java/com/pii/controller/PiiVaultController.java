package com.pii.controller;

import com.pii.dto.PiiDataRequest;
import com.pii.dto.PiiDataResponse;
import com.pii.model.PiiData;
import com.pii.service.PiiVaultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/pii")
public class PiiVaultController {

    private final PiiVaultService piiVaultService;

    public PiiVaultController(PiiVaultService piiVaultService) {
        this.piiVaultService = piiVaultService;
    }

    @PostMapping("/store")
    public ResponseEntity<PiiDataResponse> storePiiData(@Valid @RequestBody PiiDataRequest request) {
        PiiData piiData = piiVaultService.storePiiData(
            request.getDataType(),
            request.getValue(),
            request.getOwnerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(piiData));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PiiDataResponse> getPiiData(@PathVariable Long id) {
        Optional<PiiData> piiData = piiVaultService.getPiiDataById(id);
        return piiData.map(data -> ResponseEntity.ok(convertToResponse(data)))
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<PiiDataResponse>> getPiiDataByOwner(@PathVariable String ownerId) {
        List<PiiData> piiDataList = piiVaultService.getPiiDataByOwner(ownerId);
        List<PiiDataResponse> responses = piiDataList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/type/{dataType}")
    public ResponseEntity<List<PiiDataResponse>> getPiiDataByType(@PathVariable String dataType) {
        List<PiiData> piiDataList = piiVaultService.getPiiDataByType(dataType);
        List<PiiDataResponse> responses = piiDataList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePiiData(@PathVariable Long id) {
        Optional<PiiData> piiData = piiVaultService.getPiiDataById(id);
        if (piiData.isPresent()) {
            piiVaultService.deletePiiData(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private PiiDataResponse convertToResponse(PiiData piiData) {
        return new PiiDataResponse(
            piiData.getId(),
            piiData.getDataType(),
            piiData.getOwnerId(),
            piiData.getCreatedAt(),
            piiData.getUpdatedAt()
        );
    }
}
