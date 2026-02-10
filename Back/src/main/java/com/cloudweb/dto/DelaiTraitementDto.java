// DelaiTraitementDto.java
package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelaiTraitementDto {
    private Long signalementId;
    private String description;
    private String typeSignalement;
    private String statutActuel;
    private Integer delaiJours; // Délai total en jours
    private Integer delaiEtapeJours; // Délai de l'étape actuelle
    private String entrepriseAssociee;
    private String dateCreation;
    private String dateDerniereModification;
}