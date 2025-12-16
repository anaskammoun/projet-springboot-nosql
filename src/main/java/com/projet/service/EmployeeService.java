package com.projet.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.projet.entity.Employee;
import com.projet.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<Employee> findByAvailable(boolean available) {
        return employeeRepository.findByAvailable(available);
    }

    public Employee findByCin(String cin) {
        return employeeRepository.findByCin(cin);
    }

    public Employee save(Employee e) {
        return employeeRepository.save(e);
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(String id) {
        return employeeRepository.findById(id).orElse(null);
    }

    public void delete(String id) {
        employeeRepository.deleteById(id);
    }
}
