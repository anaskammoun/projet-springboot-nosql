package com.projet.service;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.projet.entity.Vehicle;
import com.projet.repository.VehicleRepository;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository repo;

    public Vehicle save(Vehicle v) { return repo.save(v); }
    public List<Vehicle> findAll() { return repo.findAll(); }
    public Vehicle findById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }
}