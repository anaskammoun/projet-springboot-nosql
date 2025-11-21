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
}
