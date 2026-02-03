package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementsStatsDto {
    private long totalPoints;
    private double totalSurface;
    private double totalBudget;
    private double advancementPercent;
}
