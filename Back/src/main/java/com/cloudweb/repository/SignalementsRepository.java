package com.cloudweb.repository;

import com.cloudweb.dto.DelaiTraitementDto;
import com.cloudweb.dto.DelaiTravauxDto;
import com.cloudweb.entity.Point;
import com.cloudweb.entity.Signalements;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

        // Requête pour calculer les délais de traitement
        @Query("""
                            SELECT new com.cloudweb.dto.DelaiTraitementDto(
                                s.id,
                                s.description,
                                ts.libelle,
                                st.libelle,
                                cast((timestampdiff(minute, s.date, CURRENT_TIMESTAMP) + 1439) / 1440 as integer),
                                cast((timestampdiff(minute, COALESCE(hs.date, s.date), CURRENT_TIMESTAMP) + 1439) / 1440 as integer),
                                e.name,
                                TO_CHAR(s.date, 'DD/MM/YYYY'),
                                TO_CHAR(s.updatedAt, 'DD/MM/YYYY HH24:MI')
                            )
                            FROM Signalements s
                            LEFT JOIN s.typeSignalement ts
                            LEFT JOIN s.statuts st
                            LEFT JOIN s.entreprise e
                            LEFT JOIN HistoriqueSignalements hs ON hs.signalements.id = s.id
                                AND hs.statuts.id = st.id
                                AND hs.date = (
                                    SELECT MAX(hs2.date)
                                    FROM HistoriqueSignalements hs2
                                    WHERE hs2.signalements.id = s.id
                                    AND hs2.statuts.id = st.id
                                )
                            WHERE s.statuts.id IS NOT NULL
                            ORDER BY s.date DESC
                        """)
        List<DelaiTraitementDto> findDelaisTraitement();

        // Version avec filtres
        @Query("""
                            SELECT new com.cloudweb.dto.DelaiTraitementDto(
                                s.id,
                                s.description,
                                ts.libelle,
                                st.libelle,
                                cast((timestampdiff(minute, s.date, CURRENT_TIMESTAMP) + 1439) / 1440 as integer),
                                cast((timestampdiff(minute, COALESCE(hs.date, s.date), CURRENT_TIMESTAMP) + 1439) / 1440 as integer),
                                e.name,
                                TO_CHAR(s.date, 'DD/MM/YYYY'),
                                TO_CHAR(s.updatedAt, 'DD/MM/YYYY HH24:MI')
                            )
                            FROM Signalements s
                            LEFT JOIN s.typeSignalement ts
                            LEFT JOIN s.statuts st
                            LEFT JOIN s.entreprise e
                            LEFT JOIN HistoriqueSignalements hs ON hs.signalements.id = s.id
                                AND hs.statuts.id = st.id
                                AND hs.date = (
                                    SELECT MAX(hs2.date)
                                    FROM HistoriqueSignalements hs2
                                    WHERE hs2.signalements.id = s.id
                                    AND hs2.statuts.id = st.id
                                )
                            WHERE (:typeId IS NULL OR ts.id = :typeId)
                            AND (:statutId IS NULL OR st.id = :statutId)
                            AND (:entrepriseId IS NULL OR e.id = :entrepriseId)
                            AND (:dateDebut IS NULL OR s.date >= :dateDebut)
                            AND (:dateFin IS NULL OR s.date <= :dateFin)
                            ORDER BY s.date DESC
                        """)
        List<DelaiTraitementDto> findDelaisTraitementFiltres(
                        @Param("typeId") Long typeId,
                        @Param("statutId") Long statutId,
                        @Param("entrepriseId") Long entrepriseId,
                        @Param("dateDebut") LocalDateTime dateDebut,
                        @Param("dateFin") LocalDateTime dateFin);

        @Query("""
                            SELECT s
                            FROM Signalements s
                            WHERE s.entreprise IS NOT NULL
                            AND (s.statuts.libelle = 'Terminé' OR s.statuts.libelle = 'En cours')
                            AND s.date IS NOT NULL
                            ORDER BY s.date DESC
                        """)
        List<Signalements> findTravauxAvecEntreprise();

        @Query("""
                            SELECT new com.cloudweb.dto.DelaiTravauxDto(
                                s.id,
                                s.description,
                                e.name,
                                ts.libelle,
                                MIN(hs.date),
                                MAX(CASE WHEN st.libelle = 'Terminé' THEN hs.date ELSE null END),
                                NULL,
                                NULL,
                                st.libelle
                            )
                            FROM Signalements s
                            LEFT JOIN s.entreprise e
                            LEFT JOIN s.typeSignalement ts
                            LEFT JOIN s.statuts st
                            LEFT JOIN HistoriqueSignalements hs ON hs.signalements.id = s.id
                            WHERE s.entreprise IS NOT NULL
                            AND hs.statuts.libelle IN ('En cours', 'Terminé')
                            GROUP BY s.id, s.description, e.name, ts.libelle, st.libelle
                            HAVING MIN(hs.date) IS NOT NULL
                            ORDER BY MIN(hs.date) DESC
                        """)
        List<DelaiTravauxDto> findDelaisTravauxDetails();
}
