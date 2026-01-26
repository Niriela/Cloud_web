package com.cloudweb.service;

import com.cloudweb.dto.AuthResponse;
import com.cloudweb.dto.RegisterRequest;
import com.cloudweb.entity.ReglesGestion;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.User;
import com.cloudweb.entity.UserType;
import com.cloudweb.repository.ReglesGestionRepository;
import com.cloudweb.repository.StatutsUserRepository;
import com.cloudweb.repository.UserTypeRepository;
import com.cloudweb.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ReglesGestionRepository reglesGestionRepository;
    private final StatutsUserRepository statutsUserRepository;
    private final UserTypeRepository userTypeRepository;
    private final FirebaseSyncService firebaseSyncService;
    private final PasswordEncoder passwordEncoder;
    public AuthResponse loginWithFirebase(String firebaseUid, String email, String idToken) {
        User user = resolveUserByFirebase(firebaseUid, email);
        if (isBlocked(user)) {
            throw new RuntimeException("User is blocked");
        }

        return AuthResponse.builder()
                .token(idToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        throw new RuntimeException("Registration is disabled");
    }

    public User createUserByManager(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String firebaseUid = createFirebaseUser(registerRequest);
        StatutsUser actif = statutsUserRepository.findByLibelle("Actif").orElse(null);
        UserType utilisateur = userTypeRepository.findByLibelle("Utilisateur").orElse(null);

        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firebaseId(firebaseUid)
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .statutsUser(actif)
                .userType(utilisateur)
                .build();

        User saved = userRepository.save(user);
        firebaseSyncService.refreshAsync();
        return saved;
    }

    public User resolveUserByFirebase(String firebaseUid, String email) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new RuntimeException("Missing Firebase uid");
        }

        User user = userRepository.findByFirebaseId(firebaseUid).orElse(null);
        if (user != null) {
            return user;
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("User not registered");
        }

        User byEmail = userRepository.findByEmail(email).orElse(null);
        if (byEmail == null) {
            throw new RuntimeException("User not registered");
        }
        if (byEmail.getFirebaseId() != null && !firebaseUid.equals(byEmail.getFirebaseId())) {
            throw new RuntimeException("Firebase identity mismatch");
        }
        byEmail.setFirebaseId(firebaseUid);
        return userRepository.save(byEmail);
    }

    private String createFirebaseUser(RegisterRequest registerRequest) {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(registerRequest.getEmail())
                .setPassword(registerRequest.getPassword())
                .setDisplayName(registerRequest.getFirstName() + " " + registerRequest.getLastName());
        try {
            UserRecord record = FirebaseAuth.getInstance().createUser(request);
            return record.getUid();
        } catch (FirebaseAuthException ex) {
            throw new RuntimeException("Firebase user creation failed: " + ex.getMessage(), ex);
        }
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
        firebaseSyncService.refreshAsync();
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            firebaseSyncService.refreshAsync();
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
        firebaseSyncService.refreshAsync();
    }
}
