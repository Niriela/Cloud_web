package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementDetailDto {
    private Long id;
    private Double latitude;
    private Double longitude;
    private LocalDateTime date;
    private String description;
    private Double surface;
    private Double budget;
    private Long statutsId;
    private String statut;
    private Long entrepriseId;
    private String entreprise;
    private Long typeSignalementId;
    private String typeSignalement;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PhotoDto> photos;
}