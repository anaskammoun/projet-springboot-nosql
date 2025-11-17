package com.projet.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.projet.entity.Tournee;
import com.projet.repository.TourneeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourneeService {

    private final TourneeRepository repo;

    public Tournee save(Tournee t) { return repo.save(t); }
    public List<Tournee> findAll() { return repo.findAll(); }
    public Tournee findById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }
}
