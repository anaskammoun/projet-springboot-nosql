package com.projet.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.projet.entity.Employee;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    List<Employee> findByAvailable(boolean available);
    Employee findByCin(String cin);
}
