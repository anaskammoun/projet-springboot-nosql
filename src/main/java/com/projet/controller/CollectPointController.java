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
        return service.save(p);
    }

    @GetMapping
    public List<CollectPoint> getAll() {
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

        return service.save(existing);
    }
@PutMapping("/planifier")
public List<CollectPoint> planifier() {
    return service.randomizeCapacities();
}


}
