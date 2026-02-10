// PerformanceIndicators.java
package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceIndicators {
    private Double delaiMoyen7jours;
    private Double delaiMoyen30jours;
    private Double tendanceEvolution; // % d'évolution
    private String performanceCategory; // "Excellent", "Bon", "À améliorer"
}