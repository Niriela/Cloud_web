package com.cloudweb.controller;

import com.cloudweb.dto.AuthResponse;
import com.cloudweb.dto.LoginRequest;
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

    @PostMapping("/login")
    @Operation(
            summary = "Connexion locale",
            description = "Authentifie un utilisateur avec la base locale et applique les regles de blocage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion valide"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
            @ApiResponse(responseCode = "403", description = "Utilisateur bloque ou non autorise"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.loginWithPassword(request);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Access denied";
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if ("Invalid credentials".equalsIgnoreCase(message)) {
                status = HttpStatus.UNAUTHORIZED;
            } else if ("User is blocked".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("Access denied".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("User not registered".equalsIgnoreCase(message)) {
                status = HttpStatus.NOT_FOUND;
            }
            return ResponseEntity.status(status).body(Map.of("message", message));
        }
    }

    @PostMapping("/offline/login")
    @Operation(
            summary = "Connexion hors-ligne",
            description = "Authentifie un utilisateur depuis la base locale."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion valide"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
            @ApiResponse(responseCode = "403", description = "Utilisateur bloque ou non autorise"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<?> offlineLogin(@RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.loginOffline(request);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Access denied";
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if ("Invalid credentials".equalsIgnoreCase(message)) {
                status = HttpStatus.UNAUTHORIZED;
            } else if ("User is blocked".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("Access denied".equalsIgnoreCase(message)) {
                status = HttpStatus.FORBIDDEN;
            } else if ("User not registered".equalsIgnoreCase(message)) {
                status = HttpStatus.NOT_FOUND;
            }
            return ResponseEntity.status(status).body(Map.of("message", message));
        }
    }

    @PostMapping("/failed-attempt")
    @Operation(
            summary = "Enregistrer un echec de connexion",
            description = "Incremente les tentatives de connexion pour un utilisateur local."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tentative enregistree"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<?> failedAttempt(@RequestBody LoginRequest request) {
        try {
            authService.registerFailedAttemptByIdentity(request.getEmail());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "User not registered";
            HttpStatus status = "User not registered".equalsIgnoreCase(message)
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", message));
        }
    }

    @PostMapping("/login-success")
    @Operation(
            summary = "Reinitialiser les tentatives apres succes",
            description = "Remet a zero les tentatives de connexion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reinitialisation ok"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<?> loginSuccess(@RequestBody LoginRequest request) {
        try {
            authService.resetFailedAttemptsByIdentity(request.getEmail());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "User not registered";
            HttpStatus status = "User not registered".equalsIgnoreCase(message)
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", message));
        }
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
