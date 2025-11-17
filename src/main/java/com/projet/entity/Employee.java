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
    private String role;       // conducteur, agent de collecte…
    private String skill;      // tri, conduite, nettoyage…
    private boolean available; // disponibilité
}