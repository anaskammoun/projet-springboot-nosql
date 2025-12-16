package com.projet.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.projet.entity.Vehicle;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    // Recherche un véhicule par numéro d'immatriculation
    Vehicle findByMatricule(String matricule);
    
    List<Vehicle> findByAvailable(boolean available);
    
    List<Vehicle> findByTypeIgnoreCase(String type);
    
    List<Vehicle> findByAvailableAndTypeIgnoreCase(boolean available, String type);
}
