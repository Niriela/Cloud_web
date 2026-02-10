// StatsGenerales.java
package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsGenerales {
    private Double delaiMoyenGlobal;
    private Double delaiMedianGlobal;
    private Integer delaiMaxGlobal;
    private Integer delaiMinGlobal;
    private Long totalSignalements;
    private Long totalTravauxCompletes;
    private Double tauxResolution;
}