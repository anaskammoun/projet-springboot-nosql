package com.projet.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.projet.entity.CollectPoint;
import com.projet.entity.Tournee;
import com.projet.entity.Vehicle;
import com.projet.repository.CollectPointRepository;
import com.projet.repository.TourneeRepository;
import com.projet.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Version refactorée du service de planification.
 * Responsabilités séparées :
 *  - sélection du véhicule le plus adapté
 *  - planification greedy pondérée (fill% vs distance)
 *  - assignation des employés
 *  - preview vs save via flag
 *
 * Paramètres ajustables via constantes en haut de classe.
 */
@Service
@RequiredArgsConstructor
public class TourneeService {

    private static final Logger log = LoggerFactory.getLogger(TourneeService.class);

    // ==== Configurables ====
    private static final double FILL_WEIGHT = 2.0;      // poids de priorité du % de remplissage
    private static final double DIST_WEIGHT = 1.5;      // poids de pénalité par km
    private static final int TOP_POINTS_TO_CONSIDER_FOR_VEHICLE = 5;
    private static final double MIN_FILL_PERCENT_TO_VISIT = 5.0; // ignorer < 5%
    private static final int MIN_EMPLOYEES_PER_TOUR = 2;

    private final TourneeRepository repo;
    private final CollectPointRepository collectPointRepo;
    private final VehicleRepository vehicleRepo;
    private final EmployeeService employeeService;

    // Utilitaires
    private final Random rnd = new Random();

    // -----------------------
    // Basic CRUD wrappers
    // -----------------------
    public Tournee save(Tournee t) { return repo.save(t); }
    public List<Tournee> findAll() { return repo.findAll(); }
    public Tournee findById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }

    // -----------------------
    // Public plan methods
    // -----------------------

    /**
     * Planifie et sauvegarde la tournée.
     */
    public Tournee planifierIntelligent() {
        return buildAndMaybeSaveTournee(true);
    }

    /**
     * Retourne une prévisualisation (ne sauvegarde pas).
     */
    public Tournee planifierIntelligentPreview() {
        return buildAndMaybeSaveTournee(false);
    }

    // -----------------------
    // Core builder
    // -----------------------
    private Tournee buildAndMaybeSaveTournee(boolean save) {
        // 1) Charger véhicules disponibles
        List<Vehicle> vehicles = vehicleRepo.findAll().stream()
                .filter(Vehicle::isAvailable)
                .collect(Collectors.toList());
        if (vehicles.isEmpty()) {
            log.warn("Aucun véhicule disponible pour planification.");
            return null;
        }

        // 2) Charger points pertinents (non vides, valide capacity)
        List<CollectPoint> allPoints = collectPointRepo.findAll().stream()
                .filter(p -> p.getMaxCapacityLiters() > 0)
                .filter(p -> p.getCapacityLiters() > 0)
                .filter(p -> !"VIDE".equalsIgnoreCase(p.getStatus()))
                .filter(p -> fillPercent(p) >= MIN_FILL_PERCENT_TO_VISIT)
                .collect(Collectors.toList());
        if (allPoints.isEmpty()) {
            log.warn("Aucun point pertinent pour planification.");
            return null;
        }

        // 3) Trier par fill% décroissant (priorité initiale)
        List<CollectPoint> candidates = allPoints.stream()
                .sorted(Comparator.comparingDouble((CollectPoint p) -> fillPercent(p)).reversed())
                .collect(Collectors.toList());

// 4) Choisir le meilleur véhicule
Vehicle tempVehicle = selectBestVehicle(vehicles, candidates);
if (tempVehicle == null) {
    // fallback random
    tempVehicle = vehicles.get(rnd.nextInt(vehicles.size()));
    log.info("Fallback vehicle chosen randomly: {}", tempVehicle.getId());
}

// Java requires final/effectively final for lambda usage
final Vehicle chosenVehicle = tempVehicle;


        // 5) Planifier la tournée (greedy pondéré)
        GreedyResult greedy = planGreedyForVehicle(chosenVehicle, candidates);

        // 6) Si aucun point sélectionné -> essayer fallback: prendre le plus petit qui rentre
        if (greedy.selectedIds.isEmpty()) {
            Optional<CollectPoint> small = candidates.stream()
                    .filter(p -> p.getCapacityLiters() <= chosenVehicle.getCapacity())
                    .min(Comparator.comparingDouble(CollectPoint::getCapacityLiters));
            if (small.isPresent()) {
                greedy.selectedIds.add(small.get().getId());
                greedy.estimatedDistanceKm = distanceKm(chosenVehicle.getLatitude(), chosenVehicle.getLongitude(),
                        small.get().getLatitude(), small.get().getLongitude());
            } else {
                log.warn("Aucun point ne rentre dans la capacité du véhicule {}", chosenVehicle.getId());
                return null;
            }
        }

        // 7) Assigner employés (au moins 2, dont au moins 1 conducteur si possible)
        List<String> assignedEmployees = assignEmployees();

        // 8) Construire la tournée
        Tournee t = new Tournee();
        t.setDate(System.currentTimeMillis());
        t.setCollectPoints(greedy.selectedIds);
        t.setVehicleId(chosenVehicle.getId());
        t.setEmployeeIds(assignedEmployees);
        t.setStatus("planifiée");
        t.setEstimatedDistance(greedy.estimatedDistanceKm);

        // 9) Sauvegarder ou prévisualiser
        if (save) {
            // NB: on ne modifie pas la disponibilité du véhicule/employés ici; startTour() s'en occupe
            t = repo.save(t);
            log.info("Tournee planifiée et sauvegardée: id={}, vehicle={}", t.getId(), chosenVehicle.getId());
        } else {
            log.info("Tournee prévisualisée (non sauvegardée) pour vehicle={}", chosenVehicle.getId());
        }

        return t;
    }

    // -----------------------
    // Vehicle selection
    // -----------------------
    private Vehicle selectBestVehicle(List<Vehicle> vehicles, List<CollectPoint> candidates) {
        if (vehicles.size() == 1) return vehicles.get(0);

        // Pre-calc top points to consider for vehicle scoring
        List<CollectPoint> topForScoring = candidates.stream()
                .limit(TOP_POINTS_TO_CONSIDER_FOR_VEHICLE)
                .collect(Collectors.toList());

        return vehicles.stream().min(Comparator.comparingDouble(
                v -> avgDistanceToPoints(v, topForScoring)
        )).orElse(null);
    }

    private double avgDistanceToPoints(Vehicle v, List<CollectPoint> points) {
        if (points.isEmpty()) return Double.MAX_VALUE;
        return points.stream()
                .mapToDouble(p -> distanceKm(v.getLatitude(), v.getLongitude(), p.getLatitude(), p.getLongitude()))
                .filter(d -> d < Double.MAX_VALUE)
                .average()
                .orElse(Double.MAX_VALUE);
    }

    // -----------------------
    // Greedy planner
    // -----------------------
    /**
     * Planifie pour un véhicule donné en combinant priorité (fill%) et distance.
     * Score = FILL_WEIGHT * fillPercent - DIST_WEIGHT * distanceKm
     */
    private GreedyResult planGreedyForVehicle(Vehicle vehicle, List<CollectPoint> candidates) {
        double remaining = vehicle.getCapacity();
        List<String> selectedIds = new ArrayList<>();

        double curLat = vehicle.getLatitude();
        double curLon = vehicle.getLongitude();
        double totalDistanceKm = 0.0;

        // defensive copy of candidates (already sorted by fill% desc)
        List<CollectPoint> candidateList = new ArrayList<>(candidates);

        while (true) {
            final double curLatLocal = curLat;
            final double curLonLocal = curLon;
            final double remLocal = remaining;

            // feasible points that fit remaining capacity and not already selected
            List<CollectPoint> feasible = candidateList.stream()
                    .filter(p -> !selectedIds.contains(p.getId()))
                    .filter(p -> p.getCapacityLiters() <= remLocal)
                    .collect(Collectors.toList());

            if (feasible.isEmpty()) break;

            // compute score: higher is better
            CollectPoint next = feasible.stream()
                    .max(Comparator.comparingDouble((ToDoubleFunction<CollectPoint>) p -> {
                        double dist = distanceKm(curLatLocal, curLonLocal, p.getLatitude(), p.getLongitude());
                        double score = FILL_WEIGHT * fillPercent(p) - DIST_WEIGHT * dist;
                        return score;
                    }))
                    .orElse(null);

            if (next == null) break;

            // add selected
            selectedIds.add(next.getId());
            double d = distanceKm(curLat, curLon, next.getLatitude(), next.getLongitude());
            if (d < Double.MAX_VALUE) totalDistanceKm += d;

            // update
            curLat = next.getLatitude();
            curLon = next.getLongitude();
            remaining -= next.getCapacityLiters();

            // minor safety: break if remaining capacity <= 0
            if (remaining <= 0) break;
        }

        return new GreedyResult(selectedIds, totalDistanceKm);
    }

    private static class GreedyResult {
        final List<String> selectedIds;
        double estimatedDistanceKm;
        GreedyResult(List<String> selectedIds, double estimatedDistanceKm) {
            this.selectedIds = selectedIds;
            this.estimatedDistanceKm = estimatedDistanceKm;
        }
    }

    // -----------------------
    // Employee assignment
    // -----------------------
    private List<String> assignEmployees() {
        List<com.projet.entity.Employee> allEmployees = employeeService.findAll();
        List<com.projet.entity.Employee> available = allEmployees.stream()
                .filter(com.projet.entity.Employee::isAvailable)
                .collect(Collectors.toList());

        List<String> assigned = new ArrayList<>();

        // Prefer a conducteur
        Optional<com.projet.entity.Employee> conducteurOpt = available.stream()
                .filter(e -> e.getRole() != null && e.getRole().toLowerCase().contains("conducteur"))
                .findAny();
        conducteurOpt.ifPresent(c -> assigned.add(c.getId()));

        // Add first other available
        available.stream()
                .filter(e -> conducteurOpt.map(c -> !c.getId().equals(e.getId())).orElse(true))
                .findFirst().ifPresent(e -> assigned.add(e.getId()));

        // Fill to minimum employees if needed
        for (com.projet.entity.Employee e : available) {
            if (assigned.size() >= MIN_EMPLOYEES_PER_TOUR) break;
            if (!assigned.contains(e.getId())) assigned.add(e.getId());
        }

        return assigned;
    }

    // -----------------------
    // Tour lifecycle (start/terminate)
    // -----------------------
    public Tournee startTour(String tourId) {
        Tournee t = repo.findById(tourId).orElse(null);
        if (t == null) return null;
        t.setStatus("en cours");

        // mark vehicle unavailable
        if (t.getVehicleId() != null) {
            Vehicle v = vehicleRepo.findById(t.getVehicleId()).orElse(null);
            if (v != null && v.isAvailable()) {
                v.setAvailable(false);
                vehicleRepo.save(v);
            }
        }

        // mark employees unavailable
        if (t.getEmployeeIds() != null) {
            for (String empId : t.getEmployeeIds()) {
                com.projet.entity.Employee e = employeeService.findById(empId);
                if (e != null && e.isAvailable()) {
                    e.setAvailable(false);
                    employeeService.save(e);
                }
            }
        }

        return repo.save(t);
    }

    public Tournee terminerTour(String tourId) {
        Tournee t = repo.findById(tourId).orElse(null);
        if (t == null) return null;
        t.setStatus("terminée");

        // liberer vehicle
        if (t.getVehicleId() != null) {
            Vehicle v = vehicleRepo.findById(t.getVehicleId()).orElse(null);
            if (v != null && !v.isAvailable()) {
                v.setAvailable(true);
                vehicleRepo.save(v);
            }
        }

        // liberer employees
        if (t.getEmployeeIds() != null) {
            for (String empId : t.getEmployeeIds()) {
                com.projet.entity.Employee e = employeeService.findById(empId);
                if (e != null && !e.isAvailable()) {
                    e.setAvailable(true);
                    employeeService.save(e);
                }
            }
        }

        // update collect points (vider)
        if (t.getCollectPoints() != null) {
            for (String cpId : t.getCollectPoints()) {
                CollectPoint cp = collectPointRepo.findById(cpId).orElse(null);
                if (cp != null) {
                    cp.setCapacityLiters(0.0);
                    cp.setStatus("VIDE");
                    collectPointRepo.save(cp);
                }
            }
        }

        return repo.save(t);
    }

    // -----------------------
    // Utilities
    // -----------------------
    private static double fillPercent(CollectPoint p) {
        if (p.getMaxCapacityLiters() <= 0) return 0.0;
        return (p.getCapacityLiters() / p.getMaxCapacityLiters()) * 100.0;
    }

    private static boolean validCoords(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) return false;
        // Accept latitude in [-90,90] and longitude in [-180,180]
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return false;
        return true;
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (!validCoords(lat1, lon1) || !validCoords(lat2, lon2)) return Double.MAX_VALUE;
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
