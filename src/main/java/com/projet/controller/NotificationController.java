package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import com.projet.entity.Notification;
import com.projet.service.NotificationService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping
    public Notification create(@RequestBody Notification n) { return service.save(n); }

    @GetMapping
    public List<Notification> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public Notification getOne(@PathVariable String id) { return service.findById(id); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { service.delete(id); }
    @PutMapping("/{id}")
        public Notification update(@PathVariable String id, @RequestBody Notification n) {
            Notification existing = service.findById(id);
            if (existing == null) return null;

            // Mettre à jour les champs
            existing.setMessage(n.getMessage());
            existing.setTimestamp(n.getTimestamp());
            existing.setType(n.getType());
            existing.setCollectPointId(n.getCollectPointId());
            //existing.setTourneeId(n.getTourneeId());
            existing.setLatitude(n.getLatitude());
            existing.setLongitude(n.getLongitude());
            existing.setRead(n.getRead());
            

            return service.save(existing);
        }

    @PutMapping("/{id}/read")
    public Notification markRead(@PathVariable String id) {
        return service.markAsRead(id);
    }

    @PutMapping("/mark-all-read")
    public List<Notification> markAllRead() {
        return service.markAllRead();
    }

    
}