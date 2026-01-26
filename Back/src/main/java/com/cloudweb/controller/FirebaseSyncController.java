package com.cloudweb.controller;

import com.cloudweb.service.FirebaseSyncService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync/firebase")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class FirebaseSyncController {

    private final FirebaseSyncService firebaseSyncService;

    @PostMapping("/push")
    public ResponseEntity<Map<String, String>> push() {
        firebaseSyncService.pushAllToFirebase();
        return ResponseEntity.ok(Map.of("status", "ok", "action", "push"));
    }

    @PostMapping("/pull")
    public ResponseEntity<Map<String, String>> pull() {
        firebaseSyncService.pullAllFromFirebase();
        return ResponseEntity.ok(Map.of("status", "ok", "action", "pull"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh() {
        firebaseSyncService.pullAllFromFirebase();
        firebaseSyncService.pushAllToFirebase();
        return ResponseEntity.ok(Map.of("status", "ok", "action", "refresh"));
    }
}
