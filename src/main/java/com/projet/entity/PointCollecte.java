package com.projet.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "points_collecte")
@Data
public class PointCollecte {

    @Id
    private String id;

    private String localisation;
    private String typeDechet;
    private int niveauRemplissage;
    private String etat;
}
