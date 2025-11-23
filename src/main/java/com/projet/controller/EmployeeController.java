package com.projet.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import com.projet.entity.Employee;
import com.projet.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public Employee create(@RequestBody Employee e) {
        return employeeService.save(e);
    }

    @GetMapping
    public List<Employee> getAll() {
        return employeeService.findAll();
    }

    @GetMapping("/{id}")
    public Employee getOne(@PathVariable String id) {
        return employeeService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        employeeService.delete(id);
    }

    @PutMapping("/{id}")
    public Employee update(@PathVariable String id, @RequestBody Employee e) {
        Employee existing = employeeService.findById(id);
        if (existing == null) return null;

        // Mettre à jour les champs
        existing.setName(e.getName());
        existing.setRole(e.getRole());
        existing.setAvailable(e.isAvailable());
        existing.setSkills(e.getSkills()); // si tu as un champ compétences
        // ajoute d’autres champs si nécessaire

        return employeeService.save(existing);
    }

}
