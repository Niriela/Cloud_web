// DelaiTravauxStatsDto.java
package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelaiTravauxStatsDto {
    private Double moyenneDelaiJours;
    private Integer medianDelaiJours;
    private Integer maxDelaiJours;
    private Integer minDelaiJours;
    private Long totalTravauxCompletes;
    private Long totalTravauxEnCours;
    private Map<String, Double> moyenneParEntreprise;
    private Map<String, Double> moyenneParType;
    private List<DelaiTravauxDto> travauxDetails;
}