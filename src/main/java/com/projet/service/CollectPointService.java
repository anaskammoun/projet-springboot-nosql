package com.projet.service;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.projet.entity.CollectPoint;
import com.projet.repository.CollectPointRepository;

@Service
@RequiredArgsConstructor
public class CollectPointService {

    private final CollectPointRepository repo;

    public CollectPoint save(CollectPoint p) { return repo.save(p); }
    public List<CollectPoint> findAll() { return repo.findAll(); }
    public CollectPoint findById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }
}
