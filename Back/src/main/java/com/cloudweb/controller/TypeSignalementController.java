package com.cloudweb.controller;

import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.service.TypeSignalementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/type-signalements")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TypeSignalementController {

    private final TypeSignalementService typeSignalementService;

    @GetMapping
    public ResponseEntity<List<TypeSignalement>> list() {
        return ResponseEntity.ok(typeSignalementService.getAll());
    }
}
