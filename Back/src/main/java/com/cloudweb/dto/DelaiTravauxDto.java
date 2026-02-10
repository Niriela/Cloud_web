// DelaiTravauxDto.java
package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelaiTravauxDto {
    private Long signalementId;
    private String description;
    private String entreprise;
    private String typeSignalement;
    private LocalDateTime dateAffectation;
    private LocalDateTime dateResolution;
    private Long delaiJours;
    private Long delaiHeures;
    private String statutFinal;
}