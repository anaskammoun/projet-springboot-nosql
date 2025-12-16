package com.projet.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.CollectPoint;

public interface CollectPointRepository extends MongoRepository<CollectPoint, String> {
	List<CollectPoint> findByStatusIgnoreCase(String status);
	List<CollectPoint> findByWasteTypeIgnoreCase(String wasteType);
}
