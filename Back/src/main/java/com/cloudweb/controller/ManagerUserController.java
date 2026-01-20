package com.cloudweb.controller;

import com.cloudweb.dto.RegisterRequest;
import com.cloudweb.dto.UserAdminDto;
import com.cloudweb.entity.User;
import com.cloudweb.service.AuthService;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ManagerUserController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<UserAdminDto> create(@RequestBody RegisterRequest request) {
        User user = authService.createUserByManager(request);
        StatutsUser statutsUser = user.getStatutsUser();
        UserType userType = user.getUserType();
        return ResponseEntity.ok(UserAdminDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .date(user.getDate())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .statutsUser(statutsUser != null ? statutsUser.getLibelle() : null)
                .userType(userType != null ? userType.getLibelle() : null)
                .build());
    }
}
