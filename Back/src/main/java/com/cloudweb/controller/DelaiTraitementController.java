// DelaiTraitementController.java
package com.cloudweb.controller;

import com.cloudweb.dto.DelaiStatsDto;
import com.cloudweb.dto.DelaiTraitementDto;
import com.cloudweb.service.DelaiTraitementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/delais")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DelaiTraitementController {

    private final DelaiTraitementService delaiTraitementService;

    @GetMapping("/traitement")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DelaiTraitementDto>> getDelaisTraitement() {
        return ResponseEntity.ok(delaiTraitementService.getDelaisTraitement());
    }

    @GetMapping("/traitement/filtres")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DelaiTraitementDto>> getDelaisTraitementFiltres(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long statutId,
            @RequestParam(required = false) Long entrepriseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        return ResponseEntity.ok(delaiTraitementService.getDelaisTraitementFiltres(
                typeId, statutId, entrepriseId, dateDebut, dateFin));
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DelaiStatsDto> getStatistiquesDelais() {
        return ResponseEntity.ok(delaiTraitementService.getStatistiquesDelais());
    }
}
