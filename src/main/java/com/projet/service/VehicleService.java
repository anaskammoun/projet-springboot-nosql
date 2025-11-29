package com.projet.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projet.entity.Vehicle;
import com.projet.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository repo;

    // Recherche par matricule
    public Vehicle findByMatricule(String matricule) {
        return repo.findByMatricule(matricule);
    }

    // CRUD
    public Vehicle save(Vehicle v) { 
        return repo.save(v); 
    }

    public List<Vehicle> findAll() { 
        return repo.findAll(); 
    }

    public Vehicle findById(String id) { 
        return repo.findById(id).orElse(null); 
    }

    public void delete(String id) { 
        repo.deleteById(id); 
    }
}
    