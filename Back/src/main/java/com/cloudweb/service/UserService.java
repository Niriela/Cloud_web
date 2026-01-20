package com.cloudweb.service;

import com.cloudweb.dto.UserAdminDto;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.User;
import com.cloudweb.entity.UserType;
import com.cloudweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserAdminDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(this::isUtilisateur)
                .map(this::toAdminDto)
                .toList();
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
}
