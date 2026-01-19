package com.cloudweb.repository;

import com.cloudweb.entity.Signalements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignalementsRepository extends JpaRepository<Signalements, Long> {
}
