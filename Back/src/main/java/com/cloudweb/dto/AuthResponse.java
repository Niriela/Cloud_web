package com.cloudweb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Reponse d'authentification")
public class AuthResponse {
    @Schema(description = "Token Firebase (ID token)", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...")
    private String token;
    @Builder.Default
    @Schema(description = "Type de jeton", example = "Bearer")
    private String type = "Bearer";
    @Schema(description = "Identifiant utilisateur", example = "42")
    private Long id;
    @Schema(description = "Adresse email", example = "user@example.com")
    private String email;
    @Schema(description = "Prenom", example = "Amina")
    private String firstName;
    @Schema(description = "Nom", example = "Diallo")
    private String lastName;
    @Schema(description = "Expiration de session (ISO 8601)", example = "2026-01-27T01:30:00Z")
    private String expiresAt;
}
