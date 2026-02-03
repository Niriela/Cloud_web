package com.cloudweb.controller;

import com.cloudweb.entity.Entreprise;
import com.cloudweb.repository.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class EntrepriseController {

    private final EntrepriseRepository entrepriseRepository;

    @GetMapping
    public ResponseEntity<List<Entreprise>> list() {
        return ResponseEntity.ok(entrepriseRepository.findAll());
    }
}
