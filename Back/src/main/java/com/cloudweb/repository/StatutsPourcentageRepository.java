package com.cloudweb.repository;

import com.cloudweb.entity.StatutsPourcentage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatutsPourcentageRepository extends JpaRepository<StatutsPourcentage, Long> {
    Optional<StatutsPourcentage> findByStatutsId(Long statutsId);
}
