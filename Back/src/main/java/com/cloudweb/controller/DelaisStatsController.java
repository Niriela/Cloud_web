// DelaisStatsController.java
package com.cloudweb.controller;

import com.cloudweb.dto.DelaisStatsGlobalDto;
import com.cloudweb.service.DelaisStatsGlobalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats/delais")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DelaisStatsController {

    private final DelaisStatsGlobalService delaisStatsGlobalService;

    @GetMapping("/global")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DelaisStatsGlobalDto> getStatsDelaisGlobales() {
        return ResponseEntity.ok(delaisStatsGlobalService.getToutesStatsDelais());
    }

    @GetMapping("/filtres")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DelaisStatsGlobalDto> getStatsDelaisFiltres(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long entrepriseId,
            @RequestParam(required = false) Long typeSignalementId) {

        return ResponseEntity.ok(delaisStatsGlobalService.getStatsDelaisAvecFiltres(
                dateDebut, dateFin, entrepriseId, typeSignalementId));
    }

    @GetMapping("/resume")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getResumeStatsDelais() {
        DelaisStatsGlobalDto stats = delaisStatsGlobalService.getToutesStatsDelais();

        Map<String, Object> resume = Map.of(
                "delaiMoyen", stats.getStatsGenerales().getDelaiMoyenGlobal(),
                "tauxResolution", stats.getStatsGenerales().getTauxResolution(),
                "totalSignalements", stats.getStatsGenerales().getTotalSignalements(),
                "performance", stats.getPerformance().getPerformanceCategory(),
                "tendance", stats.getPerformance().getTendanceEvolution());

        return ResponseEntity.ok(resume);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        DelaisStatsGlobalDto stats = delaisStatsGlobalService.getToutesStatsDelais();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("statsGenerales", stats.getStatsGenerales());
        dashboard.put("topEntreprises", stats.getChartData().getTop5Entreprises());
        dashboard.put("evolutionMensuelle", stats.getChartData().getEvolutionMensuelle());
        dashboard.put("distributionDelai", stats.getChartData().getDistributionParDelai());

        return ResponseEntity.ok(dashboard);
    }

    // Dans DelaisStatsController.java - Ajouter
    @GetMapping("/kpi")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getKPIStats() {
        DelaisStatsGlobalDto stats = delaisStatsGlobalService.getToutesStatsDelais();

        Map<String, Object> kpis = new HashMap<>();

        // KPI Principaux
        kpis.put("delaiMoyenTraitement", String.format("%.1f jours", stats.getStatsGenerales().getDelaiMoyenGlobal()));
        kpis.put("tauxResolution", String.format("%.1f%%", stats.getStatsGenerales().getTauxResolution()));
        kpis.put("signalementsActifs", stats.getStatsGenerales().getTotalSignalements());
        kpis.put("travauxCompletes", stats.getStatsGenerales().getTotalTravauxCompletes());

        // Indicateurs de performance
        kpis.put("performance", stats.getPerformance().getPerformanceCategory());
        kpis.put("tendance", String.format("%.1f%%", stats.getPerformance().getTendanceEvolution()));

        // Dernière mise à jour
        kpis.put("derniereMiseAJour", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return ResponseEntity.ok(kpis);
    }
}
