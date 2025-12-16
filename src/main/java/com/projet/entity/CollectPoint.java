package com.projet.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "collect_points")
public class CollectPoint {

    @Id
    private String id;

    private double latitude;
    private double longitude;

    @Indexed
    private String wasteType;      // plastique / organique / verre / papier
    private double capacityLiters; // current capacity in liters
    private double maxCapacityLiters; // maximum capacity in liters
    private Double niveau;         // niveau de remplissage en pourcentage (0-100)

    @Indexed
    private String status;         // VIDE – NORMAL – PRESQUE_PLEIN – PLEIN
}
