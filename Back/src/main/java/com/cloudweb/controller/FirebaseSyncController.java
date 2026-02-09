package com.cloudweb.controller;

import com.cloudweb.service.FirebaseSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync/firebase")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Slf4j
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
    public ResponseEntity<Map<String, Object>> refresh() {
        try {
            Map<String, Long> localCountsBefore = firebaseSyncService.getLocalCounts();
            Map<String, Long> remoteCountsBefore = firebaseSyncService.getRemoteCounts();
            firebaseSyncService.mergeUsersFromFirebase();
            firebaseSyncService.mergeSignalementsFromFirebase();
            Map<String, Long> localCountsAfterMerge = firebaseSyncService.getLocalCounts();
            firebaseSyncService.pushAllToFirebase();
            Map<String, Long> remoteCountsAfter = firebaseSyncService.getRemoteCounts();
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "action", "refresh",
                    "localCountsBefore", localCountsBefore,
                    "localCountsAfterMerge", localCountsAfterMerge,
                    "remoteCountsBefore", remoteCountsBefore,
                    "remoteCountsAfter", remoteCountsAfter
            ));
        } catch (RuntimeException ex) {
            log.error("Firebase sync refresh failed", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            return ResponseEntity.ok(firebaseSyncService.firebaseHealthCheck());
        } catch (RuntimeException ex) {
            log.error("Firebase health check failed", ex);
            String message = ex.getMessage() != null ? ex.getMessage() : "Health check failed";
            return ResponseEntity.status(500).body(Map.of(
                    "overallOk", false,
                    "status", "error",
                    "message", message
            ));
        }
    }
}
