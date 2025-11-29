package com.projet.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projet.entity.Vehicle;
import com.projet.service.VehicleService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    @PostMapping
    public Vehicle create(@RequestBody Vehicle v) { 
        return service.save(v); 
    }

    @GetMapping
    public List<Vehicle> getAll() { 
        return service.findAll(); 
    }

    @GetMapping("/{id}")
    public Vehicle getOne(@PathVariable String id) { 
        return service.findById(id); 
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { 
        service.delete(id); 
    }

    @PutMapping("/{id}")
    public Vehicle update(@PathVariable String id, @RequestBody Vehicle v) {
        Vehicle existing = service.findById(id);
        if (existing == null) return null;

        // Mise à jour des champs
        existing.setMatricule(v.getMatricule());
        existing.setType(v.getType());
        existing.setAvailable(v.isAvailable());
        existing.setCapacity(v.getCapacity());

        // Nouveau : mise à jour de la localisation
        existing.setLatitude(v.getLatitude());
        existing.setLongitude(v.getLongitude());

        return service.save(existing);
    }
}
