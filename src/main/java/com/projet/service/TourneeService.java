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
import com.projet.entity.Employee;
import com.projet.dto.TourneeStatsResponse;
import com.projet.repository.CollectPointRepository;
import com.projet.repository.TourneeRepository;
import com.projet.repository.VehicleRepository;
import com.projet.repository.EmployeeRepository;

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
    private final EmployeeRepository employeeRepo;
    private final EmployeeService employeeService;

    // Utilitaires
    private final Random rnd = new Random();

    // -----------------------
    // Basic CRUD wrappers
    // -----------------------
    public Tournee save(Tournee t) {
        // Si les snapshots des points de collecte sont vides ou incomplets, les reconstruire à partir des IDs
        if (t.getCollectPointsData() == null || t.getCollectPointsData().isEmpty()) {
            // Pas de points, c'est ok
        } else {
            // Vérifier si les snapshots ont besoin d'être complétés avec les coordonnées
            List<Tournee.CollectPointSnapshot> completeSnapshots = new java.util.ArrayList<>();
            for (Tournee.CollectPointSnapshot snap : t.getCollectPointsData()) {
                CollectPoint point = collectPointRepo.findById(snap.getId()).orElse(null);
                if (point != null) {
                    // Reconstruire le snapshot avec toutes les données, incluant les coordonnées
                    Tournee.CollectPointSnapshot completeSnap = new Tournee.CollectPointSnapshot(
                        point.getId(),
                        point.getNiveau(),
                        point.getCapacityLiters(),
                        point.getStatus(),
                        point.getLatitude(),
                        point.getLongitude()
                    );
                    completeSnapshots.add(completeSnap);
                }
            }
            t.setCollectPointsData(completeSnapshots);
        }
        
        // Reconstruire les snapshots des employés avec CIN et autres données complètes
        if (t.getEmployeesData() != null && !t.getEmployeesData().isEmpty()) {
            List<Tournee.EmployeeSnapshot> completeEmployeeSnapshots = new java.util.ArrayList<>();
            for (Tournee.EmployeeSnapshot snap : t.getEmployeesData()) {
                Employee emp = employeeRepo.findById(snap.getId()).orElse(null);
                if (emp != null) {
                    // Reconstruire le snapshot avec toutes les données, incluant le CIN
                    Tournee.EmployeeSnapshot completeSnap = new Tournee.EmployeeSnapshot(
                        emp.getId(),
                        emp.getCin(),
                        snap.getSelectedSkill()  // Conserver la compétence sélectionnée originelle
                    );
                    completeEmployeeSnapshots.add(completeSnap);
                }
            }
            t.setEmployeesData(completeEmployeeSnapshots);
        }
        return repo.save(t);
    }
    public List<Tournee> findAll() { return repo.findAll(); }
    public Tournee findById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }

        public TourneeStatsResponse getStats() {
        List<Tournee> tours = repo.findAll();

        long planned = tours.stream()
            .filter(t -> t.getStatus() != null && t.getStatus().equalsIgnoreCase("planifiée"))
            .count();

        long inProgress = tours.stream()
            .filter(t -> t.getStatus() != null && t.getStatus().equalsIgnoreCase("en cours"))
            .count();

        long completed = tours.stream()
            .filter(t -> t.getStatus() != null && t.getStatus().equalsIgnoreCase("terminée"))
            .count();

        // Moyenne du taux de remplissage des conteneurs (points de collecte)
        List<CollectPoint> points = collectPointRepo.findAll();
        double avgFillRate = points.stream()
            .mapToDouble(p -> {
                if (p.getNiveau() != null && p.getNiveau() >= 0) return p.getNiveau();
                return fillPercent(p);
            })
            .filter(v -> !Double.isNaN(v) && v >= 0)
            .average()
            .orElse(0.0);

        // arrondir à une décimale pour l'affichage
        double rounded = Math.round(avgFillRate * 10.0) / 10.0;

        return new TourneeStatsResponse(
            tours.size(),
            planned,
            inProgress,
            completed,
            rounded
        );
        }

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
            throw new IllegalStateException("Aucun véhicule disponible. Veuillez rendre un véhicule disponible avant de planifier une tournée.");
        }

        // 2) Charger points pertinents (non vides, valide capacity)
        List<CollectPoint> allPoints = collectPointRepo.findAll().stream()
                .filter(p -> p.getMaxCapacityLiters() > 0)
                .filter(p -> p.getCapacityLiters() > 0)
                .filter(p -> !"VIDE".equalsIgnoreCase(p.getStatus()))
                .filter(p -> {
                    // Utiliser le champ niveau directement si disponible
                    double fillPct = (p.getNiveau() != null && p.getNiveau() > 0) ? p.getNiveau() : fillPercent(p);
                    return fillPct >= MIN_FILL_PERCENT_TO_VISIT;
                })
                .collect(Collectors.toList());
        if (allPoints.isEmpty()) {
            log.warn("Aucun point pertinent pour planification.");
            throw new IllegalStateException("Aucun point de collecte nécessite une visite. Tous les points sont vides ou ont un niveau de remplissage trop faible.");
        }

        // 3) Trier par fill% décroissant (priorité initiale)
        List<CollectPoint> candidates = allPoints.stream()
                .sorted(Comparator.comparingDouble((CollectPoint p) -> {
                    // Utiliser le champ niveau directement si disponible
                    return (p.getNiveau() != null && p.getNiveau() > 0) ? p.getNiveau() : fillPercent(p);
                }).reversed())
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

        // 5bis) Optimiser l'ordre obtenu par un 2-opt léger
        List<String> optimizedIds = optimizeRoute2Opt(chosenVehicle, greedy.selectedIds);
        greedy.selectedIds.clear();
        greedy.selectedIds.addAll(optimizedIds);
        greedy.estimatedDistanceKm = computeRouteDistance(chosenVehicle, greedy.selectedIds);

        // 6) Si aucun point sélectionné -> essayer fallback: prendre le plus petit qui rentre
        if (greedy.selectedIds.isEmpty()) {
            log.warn("Greedy n'a sélectionné aucun point. Véhicule capacité: {}, Nombre de candidats: {}", 
                     chosenVehicle.getCapacity(), candidates.size());
            
            // Log des capacités des candidats pour debug
            candidates.forEach(p -> log.debug("Point {}: capacityLiters={}, niveau={}", 
                                              p.getId(), p.getCapacityLiters(), p.getNiveau()));
            
            Optional<CollectPoint> small = candidates.stream()
                    .filter(p -> {
                        boolean fits = p.getCapacityLiters() <= chosenVehicle.getCapacity();
                        if (!fits) {
                            log.debug("Point {} ne rentre pas: capacityLiters={} > vehicleCapacity={}", 
                                     p.getId(), p.getCapacityLiters(), chosenVehicle.getCapacity());
                        }
                        return fits;
                    })
                    .min(Comparator.comparingDouble(CollectPoint::getCapacityLiters));
            
            if (small.isPresent()) {
                greedy.selectedIds.add(small.get().getId());
                greedy.estimatedDistanceKm = distanceKm(chosenVehicle.getLatitude(), chosenVehicle.getLongitude(),
                        small.get().getLatitude(), small.get().getLongitude());
                log.info("Fallback: ajout du point {} avec capacité {}", small.get().getId(), small.get().getCapacityLiters());
            } else {
                log.error("Aucun point ne rentre dans la capacité du véhicule {} (capacité: {})", 
                         chosenVehicle.getId(), chosenVehicle.getCapacity());
                throw new IllegalStateException("La capacité du véhicule disponible est insuffisante pour collecter les déchets des points de collecte. Veuillez utiliser un véhicule avec une plus grande capacité.");
            }
        }

        // 7) Assigner employés (au moins 2, dont au moins 1 conducteur si possible)
        List<String> assignedEmployees = assignEmployees();
        if (assignedEmployees.size() < MIN_EMPLOYEES_PER_TOUR) {
            throw new IllegalStateException("Pas assez d'employés disponibles. Il faut au moins " + MIN_EMPLOYEES_PER_TOUR + " employés disponibles pour créer une tournée.");
        }

        // 8) Construire la tournée avec snapshots des points
        Tournee t = new Tournee();
        t.setDate(System.currentTimeMillis());
        
        // Créer les snapshots des points de collecte pour imbrication
        List<Tournee.CollectPointSnapshot> snapshots = greedy.selectedIds.stream()
            .map(id -> collectPointRepo.findById(id).orElse(null))
            .filter(p -> p != null)
            .map(p -> new Tournee.CollectPointSnapshot(
                p.getId(),
                p.getNiveau(),
                p.getCapacityLiters(),
                p.getStatus(),
                p.getLatitude(),
                p.getLongitude()
            ))
            .collect(Collectors.toList());
        t.setCollectPointsData(snapshots);
        
        // Créer le snapshot du véhicule
        t.setVehicleData(new Tournee.VehicleSnapshot(
            chosenVehicle.getId(),
            chosenVehicle.getMatricule(),
            chosenVehicle.getType()
        ));
        
        // Créer les snapshots des employés avec selectedSkill
        List<Tournee.EmployeeSnapshot> employeeSnapshots = assignedEmployees.stream()
            .map(id -> employeeRepo.findById(id).orElse(null))
            .filter(e -> e != null)
            .map(this::toEmployeeSnapshotWithSkill)
            .collect(Collectors.toList());
        t.setEmployeesData(employeeSnapshots);
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

        // Calculer la capacité minimale requise (au moins le plus petit point)
        double minRequiredCapacity = candidates.stream()
                .mapToDouble(CollectPoint::getCapacityLiters)
                .min()
                .orElse(0.0);

        log.info("Capacité minimale requise: {} L", minRequiredCapacity);

        // Filtrer les véhicules ayant une capacité suffisante
        List<Vehicle> feasibleVehicles = vehicles.stream()
                .filter(v -> v.getCapacity() >= minRequiredCapacity)
                .collect(Collectors.toList());

        if (feasibleVehicles.isEmpty()) {
            log.warn("Aucun véhicule avec capacité suffisante (>= {} L). Utilisation de tous les véhicules.", minRequiredCapacity);
            feasibleVehicles = vehicles;
        }

        log.info("Véhicules avec capacité suffisante: {}", feasibleVehicles.size());

        // Choisir le véhicule le plus proche parmi ceux qui ont la capacité
        return feasibleVehicles.stream().min(Comparator.comparingDouble(
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

        log.info("=== Début planGreedyForVehicle ===");
        log.info("Véhicule: id={}, capacité={}, position=({}, {})", 
                 vehicle.getId(), vehicle.getCapacity(), vehicle.getLatitude(), vehicle.getLongitude());
        log.info("Nombre de candidats: {}", candidates.size());

        // defensive copy of candidates (already sorted by fill% desc)
        List<CollectPoint> candidateList = new ArrayList<>(candidates);

        int iteration = 0;
        while (true) {
            iteration++;
            final double curLatLocal = curLat;
            final double curLonLocal = curLon;
            final double remLocal = remaining;

            log.debug("Itération {}: capacité restante={}", iteration, remLocal);

            // feasible points that fit remaining capacity and not already selected
            List<CollectPoint> feasible = candidateList.stream()
                    .filter(p -> !selectedIds.contains(p.getId()))
                    .filter(p -> {
                        boolean fits = p.getCapacityLiters() <= remLocal;
                        if (!fits && selectedIds.isEmpty()) {
                            log.warn("Point {} exclu: capacityLiters={} > remaining={}", 
                                    p.getId(), p.getCapacityLiters(), remLocal);
                        }
                        return fits;
                    })
                    .collect(Collectors.toList());

            log.debug("Points faisables à l'itération {}: {}", iteration, feasible.size());
            if (feasible.isEmpty()) {
                log.info("Aucun point faisable. Fin de l'algorithme. Points sélectionnés: {}", selectedIds.size());
                break;
            }

            // compute score: higher is better
            CollectPoint next = feasible.stream()
                    .max(Comparator.comparingDouble((ToDoubleFunction<CollectPoint>) p -> {
                        double dist = distanceKm(curLatLocal, curLonLocal, p.getLatitude(), p.getLongitude());
                        // Utiliser le champ niveau directement si disponible
                        double fillPct = (p.getNiveau() != null && p.getNiveau() > 0) ? p.getNiveau() : fillPercent(p);
                        double score = FILL_WEIGHT * fillPct - DIST_WEIGHT * dist;
                        return score;
                    }))
                    .orElse(null);

            if (next == null) break;

            // add selected
            selectedIds.add(next.getId());
            double d = distanceKm(curLat, curLon, next.getLatitude(), next.getLongitude());
            if (d < Double.MAX_VALUE) totalDistanceKm += d;

            log.info("Point {} sélectionné: capacité={}, distance={} km", 
                    next.getId(), next.getCapacityLiters(), d);

            // update
            curLat = next.getLatitude();
            curLon = next.getLongitude();
            remaining -= next.getCapacityLiters();

            log.debug("Capacité restante après sélection: {}", remaining);

            // minor safety: break if remaining capacity <= 0
            if (remaining <= 0) {
                log.info("Capacité du véhicule épuisée");
                break;
            }
        }

        log.info("=== Fin planGreedyForVehicle: {} points sélectionnés ===", selectedIds.size());
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
    // 2-opt post-optimisation
    // -----------------------
    /**
     * Améliore l'ordre des points sélectionnés (TSP 2-opt) pour réduire la distance totale.
     * Complexité O(n^2) mais acceptable sur de petites tournées.
     */
    private List<String> optimizeRoute2Opt(Vehicle vehicle, List<String> pointIds) {
        if (pointIds == null || pointIds.size() < 3) return pointIds;

        List<String> bestOrder = new ArrayList<>(pointIds);
        double bestDistance = computeRouteDistance(vehicle, bestOrder);

        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < bestOrder.size() - 2; i++) {
                for (int j = i + 2; j < bestOrder.size(); j++) {
                    // éviter de casser le lien final (pas de swap du tout dernier avec le tout premier)
                    if (i == 0 && j == bestOrder.size() - 1) continue;

                    List<String> candidate = new ArrayList<>(bestOrder);
                    java.util.Collections.reverse(candidate.subList(i + 1, j + 1));

                    double candidateDistance = computeRouteDistance(vehicle, candidate);
                    if (candidateDistance + 1e-6 < bestDistance) { // tolérance numérique
                        bestOrder = candidate;
                        bestDistance = candidateDistance;
                        improved = true;
                        break; // redémarrer pour profiter des gains récents
                    }
                }
                if (improved) break;
            }
        }

        log.info("2-opt appliqué: distance avant={}, après={}, points={}", computeRouteDistance(vehicle, pointIds), bestDistance, bestOrder.size());
        return bestOrder;
    }

    /**
     * Distance totale du parcours (du véhicule vers le premier point puis entre points).
     * Ignore les points introuvables ou à coordonnées invalides.
     */
    private double computeRouteDistance(Vehicle vehicle, List<String> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) return 0.0;

        double prevLat = vehicle.getLatitude();
        double prevLon = vehicle.getLongitude();
        double total = 0.0;

        for (String id : orderedIds) {
            CollectPoint cp = collectPointRepo.findById(id).orElse(null);
            if (cp == null) continue;

            double d = distanceKm(prevLat, prevLon, cp.getLatitude(), cp.getLongitude());
            if (d < Double.MAX_VALUE) total += d;

            prevLat = cp.getLatitude();
            prevLon = cp.getLongitude();
        }
        return total;
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

        // Prefer a conducteur (check skills instead of role)
        Optional<com.projet.entity.Employee> conducteurOpt = available.stream()
                .filter(e -> e.getSkills() != null && e.getSkills().stream()
                        .anyMatch(skill -> skill.toLowerCase().contains("conducteur") || skill.toLowerCase().contains("chauffeur")))
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

    /**
     * Crée un snapshot d'employé avec la compétence sélectionnée (priorité conducteur/chauffeur).
     */
    private Tournee.EmployeeSnapshot toEmployeeSnapshotWithSkill(Employee e) {
        return new Tournee.EmployeeSnapshot(
                e.getId(),
                e.getCin(),
                chooseSelectedSkill(e)
        );
    }

    /**
     * Sélectionne une compétence: conducteur/chauffeur en priorité, sinon la première disponible.
     */
    private String chooseSelectedSkill(Employee e) {
        if (e == null || e.getSkills() == null || e.getSkills().isEmpty()) return null;
        return e.getSkills().stream()
                .filter(s -> {
                    String lower = s.toLowerCase();
                    return lower.contains("conducteur") || lower.contains("chauffeur");
                })
                .findFirst()
                .orElse(e.getSkills().get(0));
    }

    // -----------------------
    // Tour lifecycle (start/terminate)
    // -----------------------
    public Tournee startTour(String tourId) {
        Tournee t = repo.findById(tourId).orElse(null);
        if (t == null) return null;
        t.setStatus("en cours");

        // mark vehicle unavailable
        if (t.getVehicleData() != null) {
            Vehicle v = vehicleRepo.findById(t.getVehicleData().getId()).orElse(null);
            if (v != null && v.isAvailable()) {
                v.setAvailable(false);
                vehicleRepo.save(v);
            }
        }

        // mark employees unavailable
        if (t.getEmployeesData() != null) {
            for (Tournee.EmployeeSnapshot empSnap : t.getEmployeesData()) {
                com.projet.entity.Employee e = employeeService.findById(empSnap.getId());
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
        if (t.getVehicleData() != null) {
            Vehicle v = vehicleRepo.findById(t.getVehicleData().getId()).orElse(null);
            if (v != null && !v.isAvailable()) {
                v.setAvailable(true);
                vehicleRepo.save(v);
            }
        }

        // liberer employees
        if (t.getEmployeesData() != null) {
            for (Tournee.EmployeeSnapshot empSnap : t.getEmployeesData()) {
                com.projet.entity.Employee e = employeeService.findById(empSnap.getId());
                if (e != null && !e.isAvailable()) {
                    e.setAvailable(true);
                    employeeService.save(e);
                }
            }
        }

        // update collect points (vider)
        if (t.getCollectPointsData() != null) {
            for (Tournee.CollectPointSnapshot cpSnap : t.getCollectPointsData()) {
                if (cpSnap == null || cpSnap.getId() == null) continue;
                CollectPoint cp = collectPointRepo.findById(cpSnap.getId()).orElse(null);
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
