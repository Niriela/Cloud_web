package com.cloudweb.repository;

import com.cloudweb.entity.Entreprise;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    Optional<Entreprise> findByNameIgnoreCase(String name);
}
