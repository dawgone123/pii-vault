package com.pii.service;

import com.pii.model.PiiData;
import com.pii.repository.PiiDataRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PiiVaultService {

    private final PiiDataRepository piiDataRepository;
    private final EncryptionService encryptionService;

    public PiiVaultService(PiiDataRepository piiDataRepository, EncryptionService encryptionService) {
        this.piiDataRepository = piiDataRepository;
        this.encryptionService = encryptionService;
    }

    public PiiData storePiiData(String dataType, String plainValue, String ownerId) {
        String encryptedValue = encryptionService.encrypt(plainValue);
        PiiData piiData = new PiiData();
        piiData.setDataType(dataType);
        piiData.setEncryptedValue(encryptedValue);
        piiData.setOwnerId(ownerId);
        return piiDataRepository.save(piiData);
    }

    public Optional<String> retrievePiiData(Long id) {
        Optional<PiiData> piiData = piiDataRepository.findById(id);
        return piiData.map(data -> encryptionService.decrypt(data.getEncryptedValue()));
    }

    public List<PiiData> getPiiDataByOwner(String ownerId) {
        return piiDataRepository.findByOwnerId(ownerId);
    }

    public List<PiiData> getPiiDataByType(String dataType) {
        return piiDataRepository.findByDataType(dataType);
    }

    public void deletePiiData(Long id) {
        piiDataRepository.deleteById(id);
    }

    public Optional<PiiData> getPiiDataById(Long id) {
        return piiDataRepository.findById(id);
    }
}
