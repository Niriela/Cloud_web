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
    @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
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
}
