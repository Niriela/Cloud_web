package com.cloudweb.controller;

import com.cloudweb.service.FirebaseSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
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

    private final ObjectProvider<FirebaseSyncService> firebaseSyncServiceProvider;

    private FirebaseSyncService requireFirebaseSyncService() {
        FirebaseSyncService service = firebaseSyncServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException(
                    "Synchronisation Firebase desactivee. Activez FIREBASE_ENABLED=true et configurez FIREBASE_SERVICE_ACCOUNT.");
        }
        return service;
    }

    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> push() {
        try {
            FirebaseSyncService firebaseSyncService = requireFirebaseSyncService();
            firebaseSyncService.pushAllToFirebase();
            return ResponseEntity.ok(Map.of("status", "ok", "action", "push"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (RuntimeException ex) {
            log.error("Firebase sync push failed", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Firebase push failed"
            ));
        }
    }

    @PostMapping("/pull")
    public ResponseEntity<Map<String, Object>> pull() {
        try {
            FirebaseSyncService firebaseSyncService = requireFirebaseSyncService();
            firebaseSyncService.pullAllFromFirebase();
            return ResponseEntity.ok(Map.of("status", "ok", "action", "pull"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (RuntimeException ex) {
            log.error("Firebase sync pull failed", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Firebase pull failed"
            ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        FirebaseSyncService firebaseSyncService = firebaseSyncServiceProvider.getIfAvailable();
        
        // Mode offline: Firebase désactivé, retourner succès
        if (firebaseSyncService == null) {
            log.info("Firebase désactivé - Mode offline actif. Les données sont stockées localement dans PostgreSQL.");
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "action", "refresh",
                    "mode", "offline",
                    "message", "Mode hors ligne actif. Données synchronisées localement dans PostgreSQL."
            ));
        }
        
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
                    "mode", "online",
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
            FirebaseSyncService firebaseSyncService = requireFirebaseSyncService();
            return ResponseEntity.ok(firebaseSyncService.firebaseHealthCheck());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "overallOk", false,
                    "status", "error",
                    "message", ex.getMessage()
            ));
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
