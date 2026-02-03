package com.cloudweb.repository;

import com.cloudweb.entity.Point;
import com.cloudweb.entity.Signalements;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignalementsRepository extends JpaRepository<Signalements, Long> {
    Optional<Signalements> findFirstByPointAndTypeSignalementAndUserAndDate(
            Point point,
            TypeSignalement typeSignalement,
            User user,
            LocalDateTime date);

    Optional<Signalements> findFirstByPointAndTypeSignalementAndDate(
            Point point,
            TypeSignalement typeSignalement,
            LocalDateTime date);
}
