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
public class SignalementUpdateRequest {
    private Double surface;
    private Double budget;
    private Long statutsId;
    private LocalDateTime statutDate;
    private Long entrepriseId;
    private Long typeSignalementId;
}
