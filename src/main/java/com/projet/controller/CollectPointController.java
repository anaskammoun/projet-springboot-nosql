package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.projet.entity.CollectPoint;
import com.projet.service.CollectPointService;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/collect-points")
@RequiredArgsConstructor
public class CollectPointController {

    private final CollectPointService service;

    @PostMapping
    public CollectPoint create(@RequestBody CollectPoint p) { return service.save(p); }

    @GetMapping
    public List<CollectPoint> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public CollectPoint getOne(@PathVariable String id) { return service.findById(id); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { service.delete(id); }

    @PutMapping("/{id}")
        public CollectPoint update(@PathVariable String id, @RequestBody CollectPoint p) {
            CollectPoint existing = service.findById(id);
            if (existing == null) return null;

            // Mettre à jour les champs
            existing.setWasteType(p.getWasteType());
            existing.setFillLevel(p.getFillLevel());
            existing.setStatus(p.getStatus());
            existing.setLatitude(p.getLatitude());
            existing.setLongitude(p.getLongitude());

            return service.save(existing);
        }

}