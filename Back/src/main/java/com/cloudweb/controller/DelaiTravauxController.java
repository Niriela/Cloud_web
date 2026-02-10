// DelaiTravauxController.java
package com.cloudweb.controller;

import com.cloudweb.dto.DelaiTravauxStatsDto;
import com.cloudweb.service.DelaiTravauxService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/delais-travaux")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DelaiTravauxController {

    private final DelaiTravauxService delaiTravauxService;

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DelaiTravauxStatsDto> getStatistiquesDelaisTravaux() {
        return ResponseEntity.ok(delaiTravauxService.getDelaisTravauxStatistiques());
    }

    @GetMapping("/statistiques/filtres")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DelaiTravauxStatsDto> getDelaisTravauxFiltres(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Long entrepriseId) {

        return ResponseEntity.ok(delaiTravauxService.getDelaisTravauxFiltres(dateDebut, dateFin, entrepriseId));
    }
}
