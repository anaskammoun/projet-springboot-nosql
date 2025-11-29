package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.Employee;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
}
