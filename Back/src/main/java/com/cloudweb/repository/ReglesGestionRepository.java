package com.cloudweb.repository;

import com.cloudweb.entity.ReglesGestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReglesGestionRepository extends JpaRepository<ReglesGestion, Long> {
    Optional<ReglesGestion> findByLibelle(String libelle);
}
