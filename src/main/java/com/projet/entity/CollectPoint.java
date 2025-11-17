package com.projet.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "collect_points")
public class CollectPoint {

    @Id
    private String id;

    private double latitude;
    private double longitude;

    private String wasteType;  // plastique / organique / verre / papier
    private int fillLevel;     // 0–100 %
    private String status;     // OK – PLEIN – ENDOMMAGÉ
}