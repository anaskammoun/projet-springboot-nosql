package com.projet.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Document(collection = "tournees")
public class Tournee {

    @Id
    private String id;

    private long date;

    // Imbrication des données des points de collecte (snapshot au moment de la planification)
    private List<CollectPointSnapshot> collectPointsData;
    
    // Imbrication des données du véhicule pour affichage rapide
    private VehicleSnapshot vehicleData;
    
    // Imbrication des données des employés pour affichage rapide
    private List<EmployeeSnapshot> employeesData;

    private String status;  // planifiée, en cours, terminée
    private double estimatedDistance; // km

    /**
     * Classe imbriquée pour stocker un snapshot des données critiques
     * d'un point de collecte au moment de la planification.
     * Cela évite des requêtes supplémentaires pour consulter ces infos.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectPointSnapshot {
        private String id;                // ID du point de collecte
        private Double niveau;            // Niveau de remplissage en % (0-100)
        private Double capacityLiters;    // Capacité actuelle en litres
        private String status;            // Status: VIDE, NORMAL, PRESQUE_PLEIN, PLEIN
    }

    /**
     * Classe imbriquée pour stocker un snapshot des données du véhicule.
     * Permet un affichage rapide du matricule et type sans requête DB.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleSnapshot {
        private String id;                // ID du véhicule
        private String matricule;         // Matricule du véhicule
        private String type;              // Type: Camion, Benne, etc.
    }

    /**
     * Classe imbriquée pour stocker un snapshot des données d'un employé.
     * Permet un affichage rapide du nom et compétence choisie pour cette tournée.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeeSnapshot {
        private String id;                // ID de l'employé
        private String name;              // Nom de l'employé
        private String selectedSkill;     // Compétence choisie pour cette tournée spécifique
    }
}