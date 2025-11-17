package com.projet.controller;

import com.projet.entity.PointCollecte;
import com.projet.service.PointCollecteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points-collecte")
public class PointCollecteController {

    @Autowired
    private PointCollecteService service;

    @PostMapping
    public PointCollecte save(@RequestBody PointCollecte p) {
        return service.save(p);
    }

    @GetMapping
    public List<PointCollecte> getAll() {
        return service.findAll();
    }
}
