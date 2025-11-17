package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.CollectPoint;

public interface CollectPointRepository extends MongoRepository<CollectPoint, String> {}
