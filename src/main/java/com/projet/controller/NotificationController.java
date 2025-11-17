package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import com.projet.entity.Notification;
import com.projet.service.NotificationService;

import lombok.RequiredArgsConstructor;

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
}