package com.projet.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    private String name;
    
    private String prenom;

    @Indexed(unique = true)
    private String cin;

    // Liste des compétences de l'employé
    private List<String> skills;

    @Indexed
    private boolean available; // disponibilité
    
}