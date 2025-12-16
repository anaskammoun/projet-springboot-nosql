package com.projet.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "vehicles")
public class Vehicle {

    @Id
    private String id;

    @Indexed(unique = true)
    private String matricule;    // numéro d'immatriculation
    @Indexed
    private String type;         // compacteur, benne, tri sélectif…
    private int capacity;        // capacité en litres
    @Indexed
    private boolean available;   // disponibilité

    private double latitude;     // position GPS
    private double longitude;    // position GPS
}
