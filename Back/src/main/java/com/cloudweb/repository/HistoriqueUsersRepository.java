package com.cloudweb.repository;

import com.cloudweb.entity.HistoriqueUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriqueUsersRepository extends JpaRepository<HistoriqueUsers, Long> {
}
