package com.projet.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "tournees")
public class Tournee {

    @Id
    private String id;

    private long date;

    private List<String> collectPoints; // IDs

    private String vehicleId;           
    private List<String> employeeIds;          

    private String status;  // planifiée, en cours, terminée
    private double estimatedDistance; // km

}