package com.cloudweb.repository;

import com.cloudweb.entity.HistoriqueSignalements;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriqueSignalementsRepository extends JpaRepository<HistoriqueSignalements, Long> {
    List<HistoriqueSignalements> findBySignalementsIdOrderByDateAsc(Long signalementId);
}
