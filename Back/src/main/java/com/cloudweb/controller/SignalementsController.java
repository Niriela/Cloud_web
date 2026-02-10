package com.cloudweb.controller;

import com.cloudweb.dto.PhotoSignalementDto;
import com.cloudweb.dto.SignalementMapDto;
import com.cloudweb.dto.SignalementUpdateRequest;
import com.cloudweb.dto.SignalementsStatsDto;
import com.cloudweb.service.SignalementsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/signalements")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SignalementsController {

    private final SignalementsService signalementsService;

    @GetMapping
    public ResponseEntity<List<SignalementMapDto>> listForMap(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(signalementsService.getAllForMap(status, type));
    }

    @GetMapping("/stats")
    public ResponseEntity<SignalementsStatsDto> stats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(signalementsService.getStats(status, type));
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<List<PhotoSignalementDto>> photos(@PathVariable Long id) {
        return ResponseEntity.ok(signalementsService.getPhotosBySignalementId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SignalementMapDto> update(
            @PathVariable Long id,
            @RequestBody SignalementUpdateRequest request
    ) {
        return ResponseEntity.ok(signalementsService.updateSignalement(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        signalementsService.deleteSignalement(id);
        return ResponseEntity.noContent().build();
    }
}
