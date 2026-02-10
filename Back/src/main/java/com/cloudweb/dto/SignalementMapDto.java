package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementMapDto {
    private Long id;
    private Double latitude;
    private Double longitude;
    private LocalDateTime date;
    private Double surface;
    private Double budget;
    private String description;
    private Long statutsId;
    private String statut;
    private Long entrepriseId;
    private String entreprise;
    private Long typeSignalementId;
    private String typeSignalement;
}
