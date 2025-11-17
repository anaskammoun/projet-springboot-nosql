package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.projet.entity.Vehicle;
import com.projet.service.VehicleService;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    @PostMapping
    public Vehicle create(@RequestBody Vehicle v) { return service.save(v); }

    @GetMapping
    public List<Vehicle> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public Vehicle getOne(@PathVariable String id) { return service.findById(id); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { service.delete(id); }
}