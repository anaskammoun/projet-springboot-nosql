package com.projet.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projet.entity.Employee;
import com.projet.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/me")
    public org.springframework.http.ResponseEntity<?> me(jakarta.servlet.http.HttpServletRequest request) {
        String id = request.getHeader("X-Employee-Id");
        String cin = request.getHeader("X-Employee-Cin");
        Employee emp = null;
        if (cin != null && !cin.isBlank()) {
            emp = employeeService.findByCin(cin);
        } else if (id != null && !id.isBlank()) {
            emp = employeeService.findById(id);
        }
        if (emp == null) {
            return org.springframework.http.ResponseEntity.status(401).body("Employee identity not provided or not found");
        }
        return org.springframework.http.ResponseEntity.ok(emp);
    }

    @PostMapping
    public Employee create(@RequestBody Employee e) {
        return employeeService.save(e);
    }

    @GetMapping
    public List<Employee> getAll(
            @RequestParam(name = "available", required = false) Boolean available,
            @RequestParam(name = "cin", required = false) String cin) {
        if (cin != null && !cin.isBlank()) {
            Employee emp = employeeService.findByCin(cin);
            return emp != null ? java.util.List.of(emp) : java.util.List.of();
        }
        if (available != null) {
            return employeeService.findByAvailable(available);
        }
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
        existing.setPrenom(e.getPrenom());
        existing.setCin(e.getCin());
        existing.setSkills(e.getSkills());
        existing.setAvailable(e.isAvailable());

        return employeeService.save(existing);
    }

}
