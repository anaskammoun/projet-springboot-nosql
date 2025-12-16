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
import org.springframework.web.bind.annotation.RequestParam;

import com.projet.entity.CollectPoint;
import com.projet.service.CollectPointService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/collect-points")
@RequiredArgsConstructor
public class CollectPointController {

    private final CollectPointService service;

    @PostMapping
    public CollectPoint create(@RequestBody CollectPoint p) {
        // Calculer le niveau de remplissage
        if (p.getMaxCapacityLiters() > 0) {
            double niveau = (p.getCapacityLiters() / p.getMaxCapacityLiters()) * 100;
            p.setNiveau(Math.round(niveau * 100.0) / 100.0); // arrondir à 2 décimales
        } else {
            p.setNiveau(0.0);
        }
        return service.save(p);
    }

    @GetMapping
    public List<CollectPoint> getAll(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "wasteType", required = false) String wasteType) {
        if (status != null && !status.isBlank()) {
            return service.findByStatus(status);
        }
        if (wasteType != null && !wasteType.isBlank()) {
            return service.findByWasteType(wasteType);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CollectPoint getOne(@PathVariable String id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @PutMapping("/{id}")
    public CollectPoint update(@PathVariable String id, @RequestBody CollectPoint p) {
        CollectPoint existing = service.findById(id);
        if (existing == null) return null;

        // Update fields
        existing.setWasteType(p.getWasteType());
        existing.setCapacityLiters(p.getCapacityLiters());
        existing.setMaxCapacityLiters(p.getMaxCapacityLiters());
        existing.setStatus(p.getStatus());
        existing.setLatitude(p.getLatitude());
        existing.setLongitude(p.getLongitude());
        
        // Calculer le niveau de remplissage
        if (existing.getMaxCapacityLiters() > 0) {
            double niveau = (existing.getCapacityLiters() / existing.getMaxCapacityLiters()) * 100;
            existing.setNiveau(Math.round(niveau * 100.0) / 100.0); // arrondir à 2 décimales
        } else {
            existing.setNiveau(0.0);
        }

        return service.save(existing);
    }

    @PutMapping("/planifier")
    public List<CollectPoint> planifier() {
        return service.randomizeCapacities();
    }
}
