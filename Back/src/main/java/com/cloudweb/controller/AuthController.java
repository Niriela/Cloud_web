package com.cloudweb.controller;

import com.cloudweb.dto.AuthResponse;
import com.cloudweb.service.AuthService;
import com.cloudweb.security.FirebaseUserPrincipal;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Auth", description = "Endpoints d'authentification et gestion des sessions")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/session")
    @Operation(
            summary = "Recuperer la session Firebase",
            description = "Retourne le profil local associe au token Firebase."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session valide"),
            @ApiResponse(responseCode = "401", description = "Token Firebase invalide"),
            @ApiResponse(responseCode = "403", description = "Utilisateur bloque"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<?> session(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof FirebaseUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        try {
            AuthResponse authResponse = authService.loginWithFirebase(
                    principal.getUid(),
                    principal.getEmail(),
                    token
            );
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Access denied";
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if ("User is blocked".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("Access denied".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("User not registered".equalsIgnoreCase(message)) {
                status = HttpStatus.NOT_FOUND;
            } else if ("Firebase identity mismatch".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("Missing Firebase uid".equalsIgnoreCase(message)) {
                status = HttpStatus.BAD_REQUEST;
            }
            return ResponseEntity.status(status).body(Map.of("message", message));
        }
    }

    @GetMapping("/health")
    @Operation(
            summary = "Verifier l'etat de l'API",
            description = "Retourne un message de sante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "API disponible")
    })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API is running");
    }

    @PostMapping("/reset-block")
    @Operation(
            summary = "Reinitialiser le blocage d'un utilisateur",
            description = "Reinitialise le statut et les tentatives de connexion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deblocage effectue"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
    public ResponseEntity<Map<String, String>> resetBlock(
            @Parameter(description = "Identifiant de l'utilisateur a debloquer", example = "1")
            @RequestParam Long userId
    ) {
        authService.resetUserBlock(userId);
        return ResponseEntity.ok(Map.of("status", "ok", "action", "reset-block"));
    }
}
