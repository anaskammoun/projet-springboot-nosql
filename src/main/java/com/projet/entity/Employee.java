package com.projet.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    private String name;

    // role à disctuter si on va laisser ou non
    private String role;       // conducteur, agent de collecte… 
    
    //private List<String> skills;
  
    private boolean available; // disponibilité
    
}