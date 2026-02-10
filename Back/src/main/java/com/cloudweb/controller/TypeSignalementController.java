package com.cloudweb.controller;

import com.cloudweb.dto.BudgetCalculationDto;
import com.cloudweb.dto.TypeSignalementDto;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.service.TypeSignalementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/type-signalements")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TypeSignalementController {

    private final TypeSignalementService typeSignalementService;

    /**
     * Liste tous les types de signalement
     */
    @GetMapping
    public ResponseEntity<List<TypeSignalement>> list() {
        return ResponseEntity.ok(typeSignalementService.getAll());
    }

    /**
     * Liste tous les types de signalement avec DTOs (inclut niveau et prix)
     */
    @GetMapping("/dto")
    public ResponseEntity<List<TypeSignalementDto>> listDto() {
        return ResponseEntity.ok(typeSignalementService.toDtoList(typeSignalementService.getAll()));
    }

    /**
     * Récupère un type de signalement par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TypeSignalementDto> getById(@PathVariable Long id) {
        return typeSignalementService.getById(id)
                .map(typeSignalementService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée un nouveau type de signalement (Manager only)
     */
    @PostMapping
    public ResponseEntity<TypeSignalementDto> create(@Valid @RequestBody TypeSignalementDto dto) {
        TypeSignalement created = typeSignalementService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(typeSignalementService.toDto(created));
    }

    /**
     * Met à jour un type de signalement existant (Manager only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TypeSignalementDto> update(
            @PathVariable Long id,
            @Valid @RequestBody TypeSignalementDto dto) {
        return typeSignalementService.update(id, dto)
                .map(typeSignalementService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Supprime un type de signalement (Manager only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (typeSignalementService.delete(id)) {
            return ResponseEntity.ok(Map.of("message", "Type de signalement supprimé avec succès"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Calcule le budget estimé pour un type de signalement et une surface donnée
     * Formule: prix_par_m2 * niveau * surface_m2
     */
    @GetMapping("/{id}/budget")
    public ResponseEntity<BudgetCalculationDto> calculerBudget(
            @PathVariable Long id,
            @RequestParam Double surface) {
        return typeSignalementService.calculerBudget(id, surface)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
