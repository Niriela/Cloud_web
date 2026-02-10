package com.cloudweb.service;

import com.cloudweb.dto.PhotoSignalementDto;
import com.cloudweb.dto.SignalementMapDto;
import com.cloudweb.dto.SignalementsStatsDto;
import com.cloudweb.dto.SignalementUpdateRequest;
import com.cloudweb.entity.Entreprise;
import com.cloudweb.entity.HistoriqueSignalements;
import com.cloudweb.entity.PhotoSignalement;
import com.cloudweb.entity.Point;
import com.cloudweb.entity.Signalements;
import com.cloudweb.entity.Statuts;
import com.cloudweb.entity.StatutsPourcentage;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.repository.EntrepriseRepository;
import com.cloudweb.repository.HistoriqueSignalementsRepository;
import com.cloudweb.repository.PhotoSignalementRepository;
import com.cloudweb.repository.SignalementsRepository;
import com.cloudweb.repository.StatutsRepository;
import com.cloudweb.repository.StatutsPourcentageRepository;
import com.cloudweb.repository.TypeSignalementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignalementsService {

    private final SignalementsRepository signalementsRepository;
    private final StatutsRepository statutsRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final TypeSignalementRepository typeSignalementRepository;
    private final PhotoSignalementRepository photoSignalementRepository;
    private final HistoriqueSignalementsRepository historiqueSignalementsRepository;
    private final StatutsPourcentageRepository statutsPourcentageRepository;

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
        Map<Long, Integer> percentagesByStatusId = loadStatusPercentages();
        double advancementPercent = totalPoints == 0
                ? 0.0
                : items.stream()
                .map(Signalements::getStatuts)
                .mapToDouble(statut -> progressFromStatus(statut, percentagesByStatusId))
                .average()
                .orElse(0.0);

        return SignalementsStatsDto.builder()
                .totalPoints(totalPoints)
                .totalSurface(totalSurface)
                .totalBudget(totalBudget)
                .advancementPercent(advancementPercent)
                .build();
    }

    public List<PhotoSignalementDto> getPhotosBySignalementId(Long signalementId) {
        if (!signalementsRepository.existsById(signalementId)) {
            throw new RuntimeException("Signalement not found");
        }
        return photoSignalementRepository.findBySignalementsIdOrderByIdAsc(signalementId)
                .stream()
                .map(this::toPhotoDto)
                .toList();
    }

    public SignalementMapDto updateSignalement(Long id, SignalementUpdateRequest request) {
        Signalements signalement = signalementsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement not found"));

        boolean statutChanged = false;
        Statuts newStatut = null;

        if (request.getSurface() != null) {
            signalement.setSurface(request.getSurface());
        }
        if (request.getBudget() != null) {
            signalement.setBudget(request.getBudget());
        }
        if (request.getStatutsId() != null) {
            Statuts statuts = statutsRepository.findById(request.getStatutsId())
                    .orElseThrow(() -> new RuntimeException("Statut not found"));
            Long previousStatutId = signalement.getStatuts() != null ? signalement.getStatuts().getId() : null;
            statutChanged = !java.util.Objects.equals(previousStatutId, statuts.getId());
            newStatut = statuts;
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

        Signalements saved = signalementsRepository.save(signalement);
        if (statutChanged && newStatut != null) {
            LocalDateTime now = LocalDateTime.now();
            HistoriqueSignalements historique = HistoriqueSignalements.builder()
                    .signalements(saved)
                    .statuts(newStatut)
                    .date(now)
                    .updatedAt(now)
                    .build();
            historiqueSignalementsRepository.save(historique);
        }
        return toMapDto(saved);
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
        StageDates stageDates = resolveStageDates(signalement);

        return SignalementMapDto.builder()
                .id(signalement.getId())
                .latitude(point != null ? point.getLatitude() : null)
                .longitude(point != null ? point.getLongitude() : null)
                .date(signalement.getDate())
                .dateNouveau(stageDates.dateNouveau())
                .dateEnCours(stageDates.dateEnCours())
                .dateTermine(stageDates.dateTermine())
                .surface(signalement.getSurface())
                .budget(signalement.getBudget())
                .description(signalement.getDescription())
                .statutsId(statuts != null ? statuts.getId() : null)
                .statut(statuts != null ? statuts.getLibelle() : null)
                .entrepriseId(entreprise != null ? entreprise.getId() : null)
                .entreprise(entreprise != null ? entreprise.getName() : null)
                .typeSignalementId(type != null ? type.getId() : null)
                .typeSignalement(type != null ? type.getLibelle() : null)
                .build();
    }

    private PhotoSignalementDto toPhotoDto(PhotoSignalement photoSignalement) {
        return PhotoSignalementDto.builder()
                .id(photoSignalement.getId())
                .url(photoSignalement.getUrl())
                .updatedAt(photoSignalement.getUpdatedAt())
                .build();
    }

    private StageDates resolveStageDates(Signalements signalement) {
        if (signalement == null || signalement.getId() == null) {
            return new StageDates(null, null, null);
        }
        LocalDateTime dateNouveau = null;
        LocalDateTime dateEnCours = null;
        LocalDateTime dateTermine = null;

        List<HistoriqueSignalements> history =
                historiqueSignalementsRepository.findBySignalementsIdOrderByDateAsc(signalement.getId());
        for (HistoriqueSignalements item : history) {
            if (item == null || item.getStatuts() == null) {
                continue;
            }
            LocalDateTime statusDate = item.getDate() != null ? item.getDate() : item.getUpdatedAt();
            if (statusDate == null) {
                continue;
            }
            String normalized = normalizeLabel(item.getStatuts().getLibelle());
            if (dateNouveau == null && "nouveau".equals(normalized)) {
                dateNouveau = statusDate;
            }
            if (dateEnCours == null && "en cours".equals(normalized)) {
                dateEnCours = statusDate;
            }
            if (dateTermine == null && "termine".equals(normalized)) {
                dateTermine = statusDate;
            }
        }

        if (dateNouveau == null) {
            String current = normalizeLabel(signalement.getStatuts() != null ? signalement.getStatuts().getLibelle() : null);
            if ("nouveau".equals(current)) {
                dateNouveau = signalement.getDate();
            }
        }

        return new StageDates(dateNouveau, dateEnCours, dateTermine);
    }

    private Map<Long, Integer> loadStatusPercentages() {
        Map<Long, Integer> mapping = new HashMap<>();
        for (StatutsPourcentage item : statutsPourcentageRepository.findAll()) {
            if (item == null || item.getStatuts() == null || item.getStatuts().getId() == null) {
                continue;
            }
            Integer value = item.getPourcentage();
            if (value == null) {
                continue;
            }
            mapping.put(item.getStatuts().getId(), clampPercentage(value));
        }
        return mapping;
    }

    private double progressFromStatus(Statuts statut, Map<Long, Integer> percentagesByStatusId) {
        if (statut != null && statut.getId() != null) {
            Integer configured = percentagesByStatusId.get(statut.getId());
            if (configured != null) {
                return configured.doubleValue();
            }
        }
        String value = normalizeLabel(statut != null ? statut.getLibelle() : null);
        if ("termine".equals(value)) {
            return 100.0;
        }
        if ("en cours".equals(value)) {
            return 50.0;
        }
        return 0.0;
    }

    private int clampPercentage(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("ë", "e")
                .replace("à", "a")
                .replace("â", "a")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ô", "o")
                .replace("ö", "o")
                .replace("ù", "u")
                .replace("û", "u")
                .replace("ü", "u");
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

    private record StageDates(LocalDateTime dateNouveau, LocalDateTime dateEnCours, LocalDateTime dateTermine) {
    }
}
