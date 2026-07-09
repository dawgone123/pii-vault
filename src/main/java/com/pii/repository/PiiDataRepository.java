package com.pii.repository;

import com.pii.model.PiiData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PiiDataRepository extends JpaRepository<PiiData, Long> {
    List<PiiData> findByOwnerId(String ownerId);
    List<PiiData> findByDataType(String dataType);
}
