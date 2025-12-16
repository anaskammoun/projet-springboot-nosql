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

    public List<Vehicle> findByAvailable(boolean available) {
        return repo.findByAvailable(available);
    }

    public List<Vehicle> findByType(String type) {
        return repo.findByTypeIgnoreCase(type);
    }

    public List<Vehicle> findByAvailableAndType(boolean available, String type) {
        return repo.findByAvailableAndTypeIgnoreCase(available, type);
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
    