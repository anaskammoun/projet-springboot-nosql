package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.projet.entity.Vehicle;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    // Recherche un véhicule par numéro d'immatriculation
    Vehicle findByMatricule(String matricule);
}
