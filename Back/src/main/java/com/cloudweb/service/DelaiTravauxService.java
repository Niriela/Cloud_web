// DelaiTravauxService.java
package com.cloudweb.service;

import com.cloudweb.dto.DelaiTravauxDto;
import com.cloudweb.dto.DelaiTravauxStatsDto;
import com.cloudweb.entity.HistoriqueSignalements;
import com.cloudweb.entity.Signalements;
import com.cloudweb.repository.SignalementsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DelaiTravauxService {

    private final SignalementsRepository signalementsRepository;

    public DelaiTravauxStatsDto getDelaisTravauxStatistiques() {
        List<Signalements> travaux = signalementsRepository.findTravauxAvecEntreprise();

        List<DelaiTravauxDto> details = calculerDelaisTravaux(travaux);

        return calculerStatistiques(details);
    }

    private List<DelaiTravauxDto> calculerDelaisTravaux(List<Signalements> travaux) {
        return travaux.stream()
                .map(this::convertirEnDelaiTravauxDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private DelaiTravauxDto convertirEnDelaiTravauxDto(Signalements signalement) {
        if (signalement.getEntreprise() == null) {
            return null;
        }

        // Trouver la date d'affectation (première entrée "En cours")
        Optional<HistoriqueSignalements> premiereAffectation = signalement.getHistoriqueSignalements().stream()
                .filter(h -> "En cours".equals(h.getStatuts().getLibelle()))
                .min(Comparator.comparing(HistoriqueSignalements::getDate));

        // Trouver la date de résolution (entrée "Terminé")
        Optional<HistoriqueSignalements> resolution = signalement.getHistoriqueSignalements().stream()
                .filter(h -> "Terminé".equals(h.getStatuts().getLibelle()))
                .max(Comparator.comparing(HistoriqueSignalements::getDate));

        if (premiereAffectation.isEmpty()) {
            return null;
        }

        LocalDateTime dateAffectation = premiereAffectation.get().getDate();
        LocalDateTime dateResolution = resolution.map(HistoriqueSignalements::getDate).orElse(null);

        Long delaiJours = null;
        Long delaiHeures = null;

        if (dateResolution != null) {
            Duration duree = Duration.between(dateAffectation, dateResolution);
            delaiJours = duree.toDays();
            delaiHeures = duree.toHours();
        }

        return DelaiTravauxDto.builder()
                .signalementId(signalement.getId())
                .description(signalement.getDescription())
                .entreprise(signalement.getEntreprise().getName())
                .typeSignalement(signalement.getTypeSignalement().getLibelle())
                .dateAffectation(dateAffectation)
                .dateResolution(dateResolution)
                .delaiJours(delaiJours)
                .delaiHeures(delaiHeures)
                .statutFinal(signalement.getStatuts().getLibelle())
                .build();
    }

    private DelaiTravauxStatsDto calculerStatistiques(List<DelaiTravauxDto> travaux) {
        // Travaux terminés seulement pour les calculs de délai
        List<DelaiTravauxDto> travauxTermines = travaux.stream()
                .filter(t -> t.getDateResolution() != null && t.getDelaiJours() != null)
                .collect(Collectors.toList());

        List<DelaiTravauxDto> travauxEnCours = travaux.stream()
                .filter(t -> t.getDateResolution() == null)
                .collect(Collectors.toList());

        if (travauxTermines.isEmpty()) {
            return DelaiTravauxStatsDto.builder()
                    .moyenneDelaiJours(0.0)
                    .medianDelaiJours(0)
                    .maxDelaiJours(0)
                    .minDelaiJours(0)
                    .totalTravauxCompletes(0L)
                    .totalTravauxEnCours((long) travauxEnCours.size())
                    .moyenneParEntreprise(Map.of())
                    .moyenneParType(Map.of())
                    .travauxDetails(travaux)
                    .build();
        }

        // Calcul des statistiques de base
        List<Long> delaisJours = travauxTermines.stream()
                .map(DelaiTravauxDto::getDelaiJours)
                .sorted()
                .collect(Collectors.toList());

        int totalTermines = delaisJours.size();
        long sommeDelais = delaisJours.stream().mapToLong(Long::longValue).sum();

        // Moyenne
        double moyenne = (double) sommeDelais / totalTermines;

        // Médiane
        int median;
        if (totalTermines % 2 == 0) {
            median = (int) ((delaisJours.get(totalTermines / 2 - 1) + delaisJours.get(totalTermines / 2)) / 2.0);
        } else {
            median = delaisJours.get(totalTermines / 2).intValue();
        }

        // Moyenne par entreprise
        Map<String, Double> moyenneParEntreprise = travauxTermines.stream()
                .collect(Collectors.groupingBy(
                        DelaiTravauxDto::getEntreprise,
                        Collectors.averagingDouble(DelaiTravauxDto::getDelaiJours)));

        // Moyenne par type
        Map<String, Double> moyenneParType = travauxTermines.stream()
                .collect(Collectors.groupingBy(
                        DelaiTravauxDto::getTypeSignalement,
                        Collectors.averagingDouble(DelaiTravauxDto::getDelaiJours)));

        return DelaiTravauxStatsDto.builder()
                .moyenneDelaiJours(moyenne)
                .medianDelaiJours(median)
                .maxDelaiJours(delaisJours.get(totalTermines - 1).intValue())
                .minDelaiJours(delaisJours.get(0).intValue())
                .totalTravauxCompletes((long) totalTermines)
                .totalTravauxEnCours((long) travauxEnCours.size())
                .moyenneParEntreprise(moyenneParEntreprise)
                .moyenneParType(moyenneParType)
                .travauxDetails(travaux)
                .build();
    }

    public DelaiTravauxStatsDto getDelaisTravauxFiltres(LocalDateTime dateDebut, LocalDateTime dateFin,
            Long entrepriseId) {
        // Implémentation des filtres selon vos besoins
        List<Signalements> tousTravaux = signalementsRepository.findTravauxAvecEntreprise();

        List<Signalements> travauxFiltres = tousTravaux.stream()
                .filter(t -> {
                    boolean filtreDate = true;
                    if (dateDebut != null) {
                        filtreDate = t.getDate().isAfter(dateDebut);
                    }
                    if (dateFin != null && filtreDate) {
                        filtreDate = t.getDate().isBefore(dateFin);
                    }
                    boolean filtreEntreprise = true;
                    if (entrepriseId != null) {
                        filtreEntreprise = t.getEntreprise().getId().equals(entrepriseId);
                    }
                    return filtreDate && filtreEntreprise;
                })
                .collect(Collectors.toList());

        List<DelaiTravauxDto> details = calculerDelaisTravaux(travauxFiltres);
        return calculerStatistiques(details);
    }
}