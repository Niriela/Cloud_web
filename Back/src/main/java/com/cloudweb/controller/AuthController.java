package com.cloudweb.controller;

import com.cloudweb.dto.AuthResponse;
import com.cloudweb.dto.LoginRequest;
import com.cloudweb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API is running");
    }

    @PostMapping("/reset-block")
    public ResponseEntity<Void> resetBlock(@RequestParam Long userId) {
        authService.resetUserBlock(userId);
        return ResponseEntity.noContent().build();
    }
}
