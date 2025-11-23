package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import com.projet.entity.Tournee;
import com.projet.service.TourneeService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourneeController {

    private final TourneeService service;

    @PostMapping
    public Tournee create(@RequestBody Tournee t) { return service.save(t); }

    @GetMapping
    public List<Tournee> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public Tournee getOne(@PathVariable String id) { return service.findById(id); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { service.delete(id); }
    @PutMapping("/{id}")
        public Tournee update(@PathVariable String id, @RequestBody Tournee t) {    
            Tournee existing = service.findById(id);
            if (existing == null) return null;

            // Mettre à jour les champs
            existing.setDate(t.getDate());
            existing.setVehicleId(t.getVehicleId());
            existing.setEmployeeIds(t.getEmployeeIds());
            existing.setCollectPoints(t.getCollectPoints());

            return service.save(existing);
        }       
}