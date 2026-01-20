package com.cloudweb.controller;

import com.cloudweb.dto.AuthResponse;
import com.cloudweb.dto.LoginRequest;
import com.cloudweb.dto.RegisterRequest;
import com.cloudweb.service.AuthService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Auth", description = "Endpoints d'authentification et gestion des sessions")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Authentifier un utilisateur",
            description = "Retourne un token JWT si l'email et le mot de passe sont valides."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification reussie"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
            @ApiResponse(responseCode = "403", description = "Utilisateur bloque"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Inscrire un nouvel utilisateur",
            description = "Cree un compte et retourne un token JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription reussie"),
            @ApiResponse(responseCode = "409", description = "Email deja utilise"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
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
