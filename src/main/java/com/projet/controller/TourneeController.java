package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.projet.entity.Tournee;
import com.projet.service.TourneeService;
import com.projet.dto.TourneeStatsResponse;

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
    public List<Tournee> getAll(@RequestParam(name = "status", required = false) String status) {
        List<Tournee> tours = service.findAll();
        if (status == null || status.isBlank()) return tours;
        return tours.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    @GetMapping("/stats")
    public TourneeStatsResponse getStats() { return service.getStats(); }

    @GetMapping("/{id}")
    public Tournee getOne(@PathVariable String id) { return service.findById(id); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { service.delete(id); }
    
    @PostMapping("/planifier-intelligent")
    public ResponseEntity<?> planifierIntelligent() {
        try {
            Tournee t = service.planifierIntelligent();
            return ResponseEntity.ok(t);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la planification de la tournée");
        }
    }

    @PostMapping("/planifier-intelligent/preview")
    public ResponseEntity<?> planifierIntelligentPreview() {
        try {
            Tournee t = service.planifierIntelligentPreview();
            return ResponseEntity.ok(t);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la génération de la proposition");
        }
    }
    @PutMapping("/{id}")
        public Tournee update(@PathVariable String id, @RequestBody Tournee t) {    
            Tournee existing = service.findById(id);
            if (existing == null) return null;

            // Mettre à jour les champs
            existing.setDate(t.getDate());
            existing.setCollectPointsData(t.getCollectPointsData());
            existing.setVehicleData(t.getVehicleData());
            existing.setEmployeesData(t.getEmployeesData());
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