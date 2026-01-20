package com.cloudweb.controller;

import com.cloudweb.service.FirebaseSyncService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Void> push() {
        firebaseSyncService.pushAllToFirebase();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pull")
    public ResponseEntity<Void> pull() {
        firebaseSyncService.pullAllFromFirebase();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh() {
        firebaseSyncService.refreshAsync();
        return ResponseEntity.accepted().build();
    }
}
