// DelaiStatsDto.java
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
public class DelaiStatsDto {
    private Double moyenneDelai;     
    private Integer medianDelai;
    private Integer maxDelai;
    private Integer minDelai;
    private Long totalSignalements;
    private Map<String, Long> repartitionParStatut;
    private Map<String, Long> repartitionParEntreprise;
}