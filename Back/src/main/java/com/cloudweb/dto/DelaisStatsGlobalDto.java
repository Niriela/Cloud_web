package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelaisStatsGlobalDto {
    private StatsGenerales statsGenerales;

    private Map<String, Double> moyenneDelaiParStatut;
    private Map<String, Double> moyenneDelaiParType;
    private Map<String, Double> moyenneDelaiParEntreprise;
    private Map<String, Long> nombreSignalementsParMois;

    private PerformanceIndicators performance;

    private ChartData chartData;
}