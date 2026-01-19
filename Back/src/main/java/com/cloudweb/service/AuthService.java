package com.cloudweb.service;

import com.cloudweb.dto.AuthResponse;
import com.cloudweb.dto.LoginRequest;
import com.cloudweb.dto.RegisterRequest;
import com.cloudweb.entity.ReglesGestion;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.User;
import com.cloudweb.repository.ReglesGestionRepository;
import com.cloudweb.repository.StatutsUserRepository;
import com.cloudweb.repository.UserRepository;
import com.cloudweb.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ReglesGestionRepository reglesGestionRepository;
    private final StatutsUserRepository statutsUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (isBlocked(user)) {
            throw new RuntimeException("User is blocked");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            resetFailedAttempts(user);
            String token = jwtTokenProvider.generateToken(authentication);

            return AuthResponse.builder()
                    .token(token)
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .build();
        } catch (AuthenticationException ex) {
            registerFailedAttempt(user);
            throw new RuntimeException("Invalid credentials");
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        StatutsUser actif = statutsUserRepository.findByLibelle("Actif").orElse(null);
        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .statutsUser(actif)
                .build();

        userRepository.save(user);

        String token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private boolean isBlocked(User user) {
        if (user.getStatutsUser() == null || user.getStatutsUser().getLibelle() == null) {
            return false;
        }
        String status = user.getStatutsUser().getLibelle();
        return "Bloque".equalsIgnoreCase(status) || "Banni".equalsIgnoreCase(status);
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        attempts++;
        user.setFailedLoginAttempts(attempts);

        int maxAttempts = resolveMaxAttempts();
        if (attempts >= maxAttempts) {
            StatutsUser bloque = statutsUserRepository.findByLibelle("Bloque").orElse(null);
            if (bloque != null) {
                user.setStatutsUser(bloque);
            }
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    private int resolveMaxAttempts() {
        try {
            ReglesGestion regle = reglesGestionRepository
                    .findByLibelle("Nombre_tentative_connexion")
                    .orElse(null);
            if (regle == null || regle.getValeur() == null) {
                return 3;
            }
            int value = Integer.parseInt(regle.getValeur());
            return value > 0 ? value : 3;
        } catch (Exception ex) {
            return 3;
        }
    }

    public void resetUserBlock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StatutsUser actif = statutsUserRepository.findByLibelle("Actif").orElse(null);
        if (actif != null) {
            user.setStatutsUser(actif);
        }
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }
}
