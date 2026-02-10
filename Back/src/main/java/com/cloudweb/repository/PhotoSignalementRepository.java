package com.cloudweb.repository;

import com.cloudweb.entity.PhotoSignalement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoSignalementRepository extends JpaRepository<PhotoSignalement, Long> {
    List<PhotoSignalement> findBySignalementsIdOrderByIdAsc(Long signalementId);

    void deleteBySignalementsId(Long signalementId);
}
