package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.projet.entity.CollectPoint;
import com.projet.service.CollectPointService;

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
}