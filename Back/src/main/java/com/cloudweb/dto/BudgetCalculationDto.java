package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetCalculationDto {
    
    private Long typeSignalementId;
    private String typeSignalementLibelle;
    private Integer niveau;
    private BigDecimal prixParM2;
    private Double surfaceM2;
    private BigDecimal budgetEstime;
    
    /**
     * Description de la formule utilisée
     */
    private String formule;
}
