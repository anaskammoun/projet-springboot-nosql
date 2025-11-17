package com.projet.service.impl;

import com.projet.entity.PointCollecte;
import com.projet.repository.PointCollecteRepository;
import com.projet.service.PointCollecteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointCollecteServiceImpl implements PointCollecteService {

    @Autowired
    private PointCollecteRepository repo;

    @Override
    public PointCollecte save(PointCollecte p) {
        return repo.save(p);
    }

    @Override
    public List<PointCollecte> findAll() {
        return repo.findAll();
    }
}
