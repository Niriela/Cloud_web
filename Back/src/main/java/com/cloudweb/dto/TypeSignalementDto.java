package com.cloudweb.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeSignalementDto {
    
    private Long id;
    
    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;
    
    @NotNull(message = "Le niveau est obligatoire")
    @Min(value = 1, message = "Le niveau minimum est 1")
    @Max(value = 10, message = "Le niveau maximum est 10")
    private Integer niveau;
    
    @NotNull(message = "Le prix par m² est obligatoire")
    @Min(value = 0, message = "Le prix par m² doit être positif")
    private BigDecimal prixParM2;
}
