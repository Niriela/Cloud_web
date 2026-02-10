// DelaiTraitementService.java
package com.cloudweb.service;

import com.cloudweb.dto.DelaiTraitementDto;
import com.cloudweb.dto.DelaiStatsDto;
import com.cloudweb.repository.SignalementsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DelaiTraitementService {

    private final SignalementsRepository signalementsRepository;

    public List<DelaiTraitementDto> getDelaisTraitement() {
        return signalementsRepository.findDelaisTraitement();
    }

    public List<DelaiTraitementDto> getDelaisTraitementFiltres(
            Long typeId, Long statutId, Long entrepriseId,
            LocalDate dateDebut, LocalDate dateFin) {

        LocalDateTime debut = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime fin = dateFin != null ? dateFin.plusDays(1).atStartOfDay() : null;

        return signalementsRepository.findDelaisTraitementFiltres(
                typeId, statutId, entrepriseId, debut, fin);
    }

    // DelaiTraitementService.java - Section corrigée
    public DelaiStatsDto getStatistiquesDelais() {
        List<DelaiTraitementDto> delais = getDelaisTraitement();

        if (delais.isEmpty()) {
            return DelaiStatsDto.builder()
                    .moyenneDelai(0.0) // Utiliser 0.0 au lieu de 0
                    .medianDelai(0) // OK - Integer
                    .maxDelai(0) // OK - Integer
                    .minDelai(0) // OK - Integer
                    .totalSignalements(0L) // Utiliser 0L au lieu de 0
                    .repartitionParStatut(Map.of())
                    .repartitionParEntreprise(Map.of())
                    .build();
        }

        List<Integer> joursList = delais.stream()
                .map(DelaiTraitementDto::getDelaiJours)
                .sorted()
                .collect(Collectors.toList());

        int total = joursList.size();
        long totalLong = total; // Convertir en long pour l'utilisation
        int somme = joursList.stream().mapToInt(Integer::intValue).sum();

        // Calcul de la médiane
        int median;
        if (total % 2 == 0) {
            median = (joursList.get(total / 2 - 1) + joursList.get(total / 2)) / 2;
        } else {
            median = joursList.get(total / 2);
        }

        return DelaiStatsDto.builder()
                .moyenneDelai(total > 0 ? (double) somme / total : 0.0) // Conversion en double
                .medianDelai(median)
                .maxDelai(joursList.get(total - 1))
                .minDelai(joursList.get(0))
                .totalSignalements(totalLong) // Utiliser le long
                .repartitionParStatut(getRepartitionParStatut(delais))
                .repartitionParEntreprise(getRepartitionParEntreprise(delais))
                .build();
    }

    private Map<String, Long> getRepartitionParStatut(List<DelaiTraitementDto> delais) {
        return delais.stream()
                .collect(Collectors.groupingBy(
                        DelaiTraitementDto::getStatutActuel,
                        Collectors.counting()));
    }

    private Map<String, Long> getRepartitionParEntreprise(List<DelaiTraitementDto> delais) {
        return delais.stream()
                .filter(d -> d.getEntrepriseAssociee() != null)
                .collect(Collectors.groupingBy(
                        DelaiTraitementDto::getEntrepriseAssociee,
                        Collectors.counting()));
    }
}