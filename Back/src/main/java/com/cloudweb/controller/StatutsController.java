package com.cloudweb.controller;

import com.cloudweb.entity.Statuts;
import com.cloudweb.repository.StatutsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statuts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class StatutsController {

    private final StatutsRepository statutsRepository;

    @GetMapping
    public ResponseEntity<List<Statuts>> list() {
        return ResponseEntity.ok(statutsRepository.findAll());
    }
}
