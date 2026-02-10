// DelaisStatsGlobalService.java
package com.cloudweb.service;

import com.cloudweb.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DelaisStatsGlobalService {

    private final DelaiTraitementService delaiTraitementService;
    private final DelaiTravauxService delaiTravauxService;
    private final SignalementsService signalementsService;

    public DelaisStatsGlobalDto getToutesStatsDelais() {
        // Récupérer toutes les données nécessaires
        DelaiStatsDto statsSignalements = delaiTraitementService.getStatistiquesDelais();
        DelaiTravauxStatsDto statsTravaux = delaiTravauxService.getDelaisTravauxStatistiques();

        // Calculer les statistiques globales
        return DelaisStatsGlobalDto.builder()
                .statsGenerales(calculerStatsGenerales(statsSignalements, statsTravaux))
                .moyenneDelaiParStatut(calculerMoyenneParStatut())
                .moyenneDelaiParType(calculerMoyenneParType())
                .moyenneDelaiParEntreprise(calculerMoyenneParEntreprise())
                .nombreSignalementsParMois(calculerSignalementsParMois())
                .performance(calculerIndicateursPerformance())
                .chartData(preparerDonneesGraphiques())
                .build();
    }

    public DelaisStatsGlobalDto getStatsDelaisAvecFiltres(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long entrepriseId,
            Long typeSignalementId) {

        // Implémentation des filtres (à adapter selon vos repositories)
        // Pour l'instant, retourner les stats globales
        return getToutesStatsDelais();
    }

    private StatsGenerales calculerStatsGenerales(DelaiStatsDto statsSignalements, DelaiTravauxStatsDto statsTravaux) {
        // Calcul du taux de résolution
        double tauxResolution = 0.0;
        if (statsSignalements.getTotalSignalements() > 0) {
            tauxResolution = (statsTravaux.getTotalTravauxCompletes() * 100.0) /
                    statsSignalements.getTotalSignalements();
        }

        return StatsGenerales.builder()
                .delaiMoyenGlobal(statsSignalements.getMoyenneDelai())
                .delaiMedianGlobal(statsSignalements.getMedianDelai().doubleValue())
                .delaiMaxGlobal(statsSignalements.getMaxDelai())
                .delaiMinGlobal(statsSignalements.getMinDelai())
                .totalSignalements(statsSignalements.getTotalSignalements())
                .totalTravauxCompletes(statsTravaux.getTotalTravauxCompletes())
                .tauxResolution(tauxResolution)
                .build();
    }

    private Map<String, Double> calculerMoyenneParStatut() {
        // Implémenter la logique pour calculer la moyenne par statut
        // Exemple basique avec des données fictives
        return Map.of(
                "Nouveau", 2.5,
                "En cours", 7.2,
                "Terminé", 12.8,
                "Annulé", 1.5);
    }

    private Map<String, Double> calculerMoyenneParType() {
        // Implémenter la logique pour calculer la moyenne par type
        return Map.of(
                "Route endommagée", 8.5,
                "Éclairage public", 5.2,
                "Déchets", 3.8,
                "Eau", 10.2);
    }

    private Map<String, Double> calculerMoyenneParEntreprise() {
        // Implémenter la logique pour calculer la moyenne par entreprise
        // Utiliser les données de DelaiTravauxService
        return Map.of(
                "Entreprise A", 6.5,
                "Entreprise B", 8.2,
                "Entreprise C", 4.8,
                "Entreprise D", 9.1);
    }

    private Map<String, Long> calculerSignalementsParMois() {
        // Calculer le nombre de signalements par mois pour les 12 derniers mois
        Map<String, Long> statsParMois = new TreeMap<>();
        LocalDate now = LocalDate.now();

        for (int i = 11; i >= 0; i--) {
            YearMonth mois = YearMonth.from(now.minusMonths(i));
            String moisFormate = mois.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            // Ici, vous devrez appeler votre repository pour le vrai comptage
            statsParMois.put(moisFormate, 10L + new Random().nextInt(50));
        }

        return statsParMois;
    }

    private PerformanceIndicators calculerIndicateursPerformance() {
        // Calculer les indicateurs de performance
        return PerformanceIndicators.builder()
                .delaiMoyen7jours(5.2) // Exemple
                .delaiMoyen30jours(7.8) // Exemple
                .tendanceEvolution(-12.5) // -12.5% d'amélioration
                .performanceCategory("Bon")
                .build();
    }

    private ChartData preparerDonneesGraphiques() {
        // Préparer les données pour les graphiques
        return ChartData.builder()
                .evolutionMensuelle(Map.of(
                        "Jan", 8.2, "Fév", 7.8, "Mar", 6.9,
                        "Avr", 7.2, "Mai", 6.5, "Jun", 5.9))
                .distributionParDelai(Map.of(
                        "< 3 jours", 25,
                        "3-7 jours", 40,
                        "8-14 jours", 20,
                        "> 14 jours", 15))
                .top5Entreprises(Map.of(
                        "Entreprise A", 5.2,
                        "Entreprise B", 6.8,
                        "Entreprise C", 7.1,
                        "Entreprise D", 8.5,
                        "Entreprise E", 9.2))
                .top5Types(Map.of(
                        "Route", 8.2,
                        "Éclairage", 5.8,
                        "Déchets", 4.2,
                        "Eau", 9.5,
                        "Voirie", 6.8))
                .build();
    }
}