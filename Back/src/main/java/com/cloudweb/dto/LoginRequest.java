package com.cloudweb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requete de connexion")
public class LoginRequest {
    @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
    private String email;
    @Schema(description = "Mot de passe en clair", example = "Password123!")
    private String password;
}
