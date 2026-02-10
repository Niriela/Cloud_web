package com.cloudweb.service;

import com.cloudweb.dto.BudgetCalculationDto;
import com.cloudweb.dto.TypeSignalementDto;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.repository.TypeSignalementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TypeSignalementService {

    private final TypeSignalementRepository typeSignalementRepository;

    public List<TypeSignalement> getAll() {
        return typeSignalementRepository.findAll();
    }

    public Optional<TypeSignalement> getById(Long id) {
        return typeSignalementRepository.findById(id);
    }

    public Optional<TypeSignalement> getByLibelle(String libelle) {
        return typeSignalementRepository.findByLibelleIgnoreCase(libelle);
    }

    @Transactional
    public TypeSignalement create(TypeSignalementDto dto) {
        TypeSignalement typeSignalement = TypeSignalement.builder()
                .libelle(dto.getLibelle())
                .niveau(dto.getNiveau() != null ? dto.getNiveau() : 1)
                .prixParM2(dto.getPrixParM2() != null ? dto.getPrixParM2() : BigDecimal.ZERO)
                .build();
        return typeSignalementRepository.save(typeSignalement);
    }

    @Transactional
    public Optional<TypeSignalement> update(Long id, TypeSignalementDto dto) {
        return typeSignalementRepository.findById(id)
                .map(existing -> {
                    if (dto.getLibelle() != null) {
                        existing.setLibelle(dto.getLibelle());
                    }
                    if (dto.getNiveau() != null) {
                        existing.setNiveau(dto.getNiveau());
                    }
                    if (dto.getPrixParM2() != null) {
                        existing.setPrixParM2(dto.getPrixParM2());
                    }
                    return typeSignalementRepository.save(existing);
                });
    }

    @Transactional
    public boolean delete(Long id) {
        if (typeSignalementRepository.existsById(id)) {
            typeSignalementRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Calcule le budget estimé pour un type de signalement et une surface donnée.
     * Formule: prix_par_m2 * niveau * surface_m2
     */
    public Optional<BudgetCalculationDto> calculerBudget(Long typeSignalementId, Double surfaceM2) {
        return typeSignalementRepository.findById(typeSignalementId)
                .map(type -> {
                    BigDecimal budget = type.calculerBudget(surfaceM2);
                    return BudgetCalculationDto.builder()
                            .typeSignalementId(type.getId())
                            .typeSignalementLibelle(type.getLibelle())
                            .niveau(type.getNiveau())
                            .prixParM2(type.getPrixParM2())
                            .surfaceM2(surfaceM2)
                            .budgetEstime(budget)
                            .formule(String.format("%.2f € × %d × %.2f m² = %.2f €",
                                    type.getPrixParM2(), type.getNiveau(), surfaceM2, budget))
                            .build();
                });
    }

    /**
     * Convertit une entité en DTO
     */
    public TypeSignalementDto toDto(TypeSignalement entity) {
        return TypeSignalementDto.builder()
                .id(entity.getId())
                .libelle(entity.getLibelle())
                .niveau(entity.getNiveau())
                .prixParM2(entity.getPrixParM2())
                .build();
    }

    /**
     * Convertit une liste d'entités en DTOs
     */
    public List<TypeSignalementDto> toDtoList(List<TypeSignalement> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
