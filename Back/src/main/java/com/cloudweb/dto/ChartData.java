// ChartData.java
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
public class ChartData {
    private Map<String, Double> evolutionMensuelle;
    private Map<String, Integer> distributionParDelai;
    private Map<String, Double> top5Entreprises;
    private Map<String, Double> top5Types;
}