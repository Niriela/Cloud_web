package com.cloudweb.repository;

import com.cloudweb.entity.Statuts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatutsRepository extends JpaRepository<Statuts, Long> {
}
