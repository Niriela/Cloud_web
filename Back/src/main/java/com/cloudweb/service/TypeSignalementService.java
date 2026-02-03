package com.cloudweb.service;

import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.repository.TypeSignalementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeSignalementService {

    private final TypeSignalementRepository typeSignalementRepository;

    public List<TypeSignalement> getAll() {
        return typeSignalementRepository.findAll();
    }
}
