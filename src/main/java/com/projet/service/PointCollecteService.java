package com.projet.service;

import com.projet.entity.PointCollecte;
import java.util.List;

public interface PointCollecteService {
    PointCollecte save(PointCollecte p);
    List<PointCollecte> findAll();
}