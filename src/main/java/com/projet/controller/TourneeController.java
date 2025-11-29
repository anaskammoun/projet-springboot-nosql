package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    
    @PostMapping("/planifier-intelligent")
    public ResponseEntity<Tournee> planifierIntelligent() {
        Tournee t = service.planifierIntelligent();
        if (t == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        return ResponseEntity.ok(t);
    }

    @PostMapping("/planifier-intelligent/preview")
    public ResponseEntity<Tournee> planifierIntelligentPreview() {
        Tournee t = service.planifierIntelligentPreview();
        if (t == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        return ResponseEntity.ok(t);
    }
    @PutMapping("/{id}")
        public Tournee update(@PathVariable String id, @RequestBody Tournee t) {    
            Tournee existing = service.findById(id);
            if (existing == null) return null;

            // Mettre à jour les champs
            existing.setDate(t.getDate());
            existing.setVehicleId(t.getVehicleId());
            existing.setEmployeeIds(t.getEmployeeIds());
            existing.setCollectPoints(t.getCollectPoints());
            // Persist status and estimatedDistance coming from the client
            existing.setStatus(t.getStatus());
            existing.setEstimatedDistance(t.getEstimatedDistance());

            return service.save(existing);
        }       
    @PostMapping("/{id}/start")
    public ResponseEntity<Tournee> startTour(@PathVariable String id) {
        Tournee t = service.startTour(id);
        if (t == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        return ResponseEntity.ok(t);
    }
    @PostMapping("/{id}/finish")
    public ResponseEntity<Tournee> finishTour(@PathVariable String id) {
        Tournee t = service.terminerTour(id);
        if (t == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        return ResponseEntity.ok(t);
    }
}