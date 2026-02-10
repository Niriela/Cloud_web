package com.cloudweb.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoSignalementDto {
    private Long id;
    private String url;
    private LocalDateTime updatedAt;
}
