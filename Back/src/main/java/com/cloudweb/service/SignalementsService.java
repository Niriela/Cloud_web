package com.cloudweb.service;

import com.cloudweb.dto.SignalementMapDto;
import com.cloudweb.dto.SignalementsStatsDto;
import com.cloudweb.dto.SignalementUpdateRequest;
import com.cloudweb.entity.Entreprise;
import com.cloudweb.entity.Point;
import com.cloudweb.entity.Signalements;
import com.cloudweb.entity.Statuts;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.repository.EntrepriseRepository;
import com.cloudweb.repository.SignalementsRepository;
import com.cloudweb.repository.StatutsRepository;
import com.cloudweb.repository.TypeSignalementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SignalementsService {

    private final SignalementsRepository signalementsRepository;
    private final StatutsRepository statutsRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final TypeSignalementRepository typeSignalementRepository;

    public List<SignalementMapDto> getAllForMap(String statusFilter, String typeFilter) {
        return signalementsRepository.findAll()
                .stream()
                .filter(item -> matchesFilters(item, statusFilter, typeFilter))
                .map(this::toMapDto)
                .toList();
    }

    public SignalementsStatsDto getStats(String statusFilter, String typeFilter) {
        List<Signalements> items = signalementsRepository.findAll()
                .stream()
                .filter(item -> matchesFilters(item, statusFilter, typeFilter))
                .toList();
        long totalPoints = items.size();
        double totalSurface = items.stream()
                .map(Signalements::getSurface)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalBudget = items.stream()
                .map(Signalements::getBudget)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        long completed = items.stream()
                .filter(this::isCompleted)
                .count();
        double advancementPercent = totalPoints == 0
                ? 0.0
                : (completed * 100.0) / totalPoints;

        return SignalementsStatsDto.builder()
                .totalPoints(totalPoints)
                .totalSurface(totalSurface)
                .totalBudget(totalBudget)
                .advancementPercent(advancementPercent)
                .build();
    }

    public SignalementMapDto updateSignalement(Long id, SignalementUpdateRequest request) {
        Signalements signalement = signalementsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement not found"));

        if (request.getSurface() != null) {
            signalement.setSurface(request.getSurface());
        }
        if (request.getBudget() != null) {
            signalement.setBudget(request.getBudget());
        }
        if (request.getStatutsId() != null) {
            Statuts statuts = statutsRepository.findById(request.getStatutsId())
                    .orElseThrow(() -> new RuntimeException("Statut not found"));
            signalement.setStatuts(statuts);
        }
        if (request.getEntrepriseId() != null) {
            Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                    .orElseThrow(() -> new RuntimeException("Entreprise not found"));
            signalement.setEntreprise(entreprise);
        }
        if (request.getTypeSignalementId() != null) {
            TypeSignalement typeSignalement = typeSignalementRepository
                    .findById(request.getTypeSignalementId())
                    .orElseThrow(() -> new RuntimeException("Type signalement not found"));
            signalement.setTypeSignalement(typeSignalement);
        }

        return toMapDto(signalementsRepository.save(signalement));
    }

    public void deleteSignalement(Long id) {
        Signalements signalement = signalementsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement not found"));
        signalementsRepository.delete(signalement);
    }

    private SignalementMapDto toMapDto(Signalements signalement) {
        Point point = signalement.getPoint();
        Statuts statuts = signalement.getStatuts();
        Entreprise entreprise = signalement.getEntreprise();
        TypeSignalement type = signalement.getTypeSignalement();

        return SignalementMapDto.builder()
                .id(signalement.getId())
                .latitude(point != null ? point.getLatitude() : null)
                .longitude(point != null ? point.getLongitude() : null)
                .date(signalement.getDate())
                .surface(signalement.getSurface())
                .budget(signalement.getBudget())
                .statutsId(statuts != null ? statuts.getId() : null)
                .statut(statuts != null ? statuts.getLibelle() : null)
                .entrepriseId(entreprise != null ? entreprise.getId() : null)
                .entreprise(entreprise != null ? entreprise.getName() : null)
                .typeSignalementId(type != null ? type.getId() : null)
                .typeSignalement(type != null ? type.getLibelle() : null)
                .build();
    }

    private boolean isCompleted(Signalements signalement) {
        Statuts statuts = signalement.getStatuts();
        if (statuts == null || statuts.getLibelle() == null) {
            return false;
        }
        String value = statuts.getLibelle().trim().toLowerCase();
        return "terminé".equals(value) || "termine".equals(value);
    }

    private boolean matchesFilters(Signalements signalement, String statusFilter, String typeFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            Statuts statuts = signalement.getStatuts();
            String current = statuts != null ? statuts.getLibelle() : null;
            if (!equalsIgnoreCase(current, statusFilter)) {
                return false;
            }
        }

        if (typeFilter != null && !typeFilter.isBlank()) {
            TypeSignalement typeSignalement = signalement.getTypeSignalement();
            String current = typeSignalement != null ? typeSignalement.getLibelle() : null;
            if (!equalsIgnoreCase(current, typeFilter)) {
                return false;
            }
        }

        return true;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().toLowerCase(Locale.ROOT)
                .equals(right.trim().toLowerCase(Locale.ROOT));
    }
}
