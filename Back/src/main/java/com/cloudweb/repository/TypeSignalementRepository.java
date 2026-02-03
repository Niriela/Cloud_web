package com.cloudweb.repository;

import com.cloudweb.entity.TypeSignalement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeSignalementRepository extends JpaRepository<TypeSignalement, Long> {
    Optional<TypeSignalement> findByLibelleIgnoreCase(String libelle);
}
