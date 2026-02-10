package com.cloudweb.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeSignalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;

    @Min(1)
    @Max(10)
    @Column(nullable = false)
    @Builder.Default
    private Integer niveau = 1;

    @Column(name = "prix_par_m2", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal prixParM2 = BigDecimal.ZERO;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (niveau == null) {
            niveau = 1;
        }
        if (prixParM2 == null) {
            prixParM2 = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calcule le budget estimé pour une surface donnée.
     * Formule: prix_par_m2 * niveau * surface_m2
     */
    public BigDecimal calculerBudget(Double surfaceM2) {
        if (surfaceM2 == null || surfaceM2 <= 0) {
            return BigDecimal.ZERO;
        }
        return prixParM2
                .multiply(BigDecimal.valueOf(niveau))
                .multiply(BigDecimal.valueOf(surfaceM2));
    }
}
