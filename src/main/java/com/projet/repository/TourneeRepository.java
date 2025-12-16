package com.projet.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.Tournee;

public interface TourneeRepository extends MongoRepository<Tournee, String> {
	List<Tournee> findByStatusIgnoreCase(String status);
	long countByStatusIgnoreCase(String status);
}