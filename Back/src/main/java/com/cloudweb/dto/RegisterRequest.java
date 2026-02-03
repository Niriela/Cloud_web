package com.cloudweb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requete d'inscription")
public class RegisterRequest {
    @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com")
    private String email;
    @Schema(description = "Mot de passe en clair", example = "Password123!")
    private String password;
    @Schema(description = "Prenom de l'utilisateur", example = "Amina")
    private String firstName;
    @Schema(description = "Nom de famille de l'utilisateur", example = "Diallo")
    private String lastName;
}
