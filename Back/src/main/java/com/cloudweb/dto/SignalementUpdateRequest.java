package com.cloudweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementUpdateRequest {
    private Double surface;
    private Double budget;
    private Long statutsId;
    private Long entrepriseId;
    private Long typeSignalementId;
}
