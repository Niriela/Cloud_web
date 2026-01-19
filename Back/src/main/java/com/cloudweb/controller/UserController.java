package com.cloudweb.controller;

import com.cloudweb.dto.UserAdminDto;
import com.cloudweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserAdminDto>> list() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
