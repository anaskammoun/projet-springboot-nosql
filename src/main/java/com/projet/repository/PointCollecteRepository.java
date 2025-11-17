package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.PointCollecte;

public interface PointCollecteRepository extends MongoRepository<PointCollecte, String> {
}
