package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.Tournee;

public interface TourneeRepository extends MongoRepository<Tournee, String> {}