package com.cloudweb.repository;

import com.cloudweb.entity.StatutsUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutsUserRepository extends JpaRepository<StatutsUser, Long> {
    Optional<StatutsUser> findByLibelle(String libelle);
}
