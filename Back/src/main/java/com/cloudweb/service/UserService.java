package com.cloudweb.service;

import com.cloudweb.dto.UserAdminDto;
import com.cloudweb.dto.UserUpdateRequest;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.User;
import com.cloudweb.entity.UserType;
import com.cloudweb.repository.UserRepository;
import com.cloudweb.service.FirebaseSyncService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FirebaseSyncService firebaseSyncService;

    public List<UserAdminDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(this::isUtilisateur)
                .map(this::toAdminDto)
                .toList();
    }

    public UserAdminDto updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmailAndIdNot(newEmail, user.getId())) {
                throw new RuntimeException("Email already exists");
            }
            if (user.getFirebaseId() != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
                updateFirebaseEmail(user.getFirebaseId(), newEmail);
            }
            user.setEmail(newEmail);
        }
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        User saved = userRepository.save(user);
        firebaseSyncService.refreshAsync();
        return toAdminDto(saved);
    }

    private UserAdminDto toAdminDto(User user) {
        StatutsUser statutsUser = user.getStatutsUser();
        UserType userType = user.getUserType();

        return UserAdminDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .date(user.getDate())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .statutsUser(statutsUser != null ? statutsUser.getLibelle() : null)
                .userType(userType != null ? userType.getLibelle() : null)
                .build();
    }

    private boolean isUtilisateur(User user) {
        UserType userType = user.getUserType();
        if (userType == null || userType.getLibelle() == null) {
            return false;
        }
        return "utilisateur".equalsIgnoreCase(userType.getLibelle());
    }

    private void updateFirebaseEmail(String firebaseId, String newEmail) {
        try {
            FirebaseAuth.getInstance().updateUser(
                    new UserRecord.UpdateRequest(firebaseId).setEmail(newEmail)
            );
        } catch (FirebaseAuthException ex) {
            throw new RuntimeException("Firebase email update failed: " + ex.getMessage(), ex);
        }
    }
}
