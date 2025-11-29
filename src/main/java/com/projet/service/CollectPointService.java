package com.projet.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projet.entity.CollectPoint;
import com.projet.repository.CollectPointRepository;
import com.projet.entity.Notification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollectPointService {

    private final CollectPointRepository repo;
    private final NotificationService notificationService;

    public CollectPoint save(CollectPoint p) {
        // read previous persisted value (if any) to detect threshold crossing
        Double previousRatio = null;
        if (p.getId() != null) {
            var existing = repo.findById(p.getId()).orElse(null);
            if (existing != null && existing.getMaxCapacityLiters() > 0) {
                previousRatio = (existing.getCapacityLiters() / existing.getMaxCapacityLiters()) * 100.0;
            }
        }

        // Always update status before saving
        updateStatus(p);

        // compute new ratio and detect crossing >80%
        Double newRatio = null;
        if (p.getMaxCapacityLiters() > 0) {
            newRatio = (p.getCapacityLiters() / p.getMaxCapacityLiters()) * 100.0;
        }

        CollectPoint saved = repo.save(p);

        try {
            if (newRatio != null && newRatio > 80.0 && (previousRatio == null || previousRatio <= 80.0)) {
                // create a notification for this collect point
                Notification n = new Notification();
                n.setType("CONTAINER_HIGH");
                String msg = String.format("Point de collecte %s (%s) dépasse 80%% — %.1f%%", saved.getId(), saved.getWasteType() == null ? "?" : saved.getWasteType(), newRatio);
                n.setMessage(msg);
                n.setCollectPointId(saved.getId());
                n.setTourneeId(null);
                n.setLatitude(saved.getLatitude());
                n.setLongitude(saved.getLongitude());
                n.setRead(false);
                n.setTimestamp(System.currentTimeMillis());

                notificationService.save(n);
            }
        } catch (Exception ex) {
            // don't fail save if notification fails
            System.err.println("Failed to create notification: " + ex.getMessage());
        }

        return saved;
    }

    public List<CollectPoint> findAll() {
        return repo.findAll();
    }

    public CollectPoint findById(String id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    // Randomize capacities + auto-status
public List<CollectPoint> randomizeCapacities() {
    List<CollectPoint> list = repo.findAll();

    list.forEach(p -> {
        // compute previous ratio before changing capacity
        double oldRatio = -1.0;
        if (p.getMaxCapacityLiters() > 0) {
            oldRatio = (p.getCapacityLiters() / p.getMaxCapacityLiters()) * 100.0;
        }

        double randomCapacity = Math.random() * p.getMaxCapacityLiters();
        // On s'assure que ça ne dépasse jamais maxCapacityLiters
        p.setCapacityLiters(Math.min(Math.round(randomCapacity), p.getMaxCapacityLiters()));
        updateStatus(p); // auto-status
        // create notification if we crossed >80% threshold
        try {
            double newRatio = -1.0;
            if (p.getMaxCapacityLiters() > 0) newRatio = (p.getCapacityLiters() / p.getMaxCapacityLiters()) * 100.0;
            if (newRatio > 80.0 && oldRatio <= 80.0) {
                Notification n = new Notification();
                n.setType("CONTAINER_HIGH");
                String msg = String.format("Point de collecte %s (%s) dépasse 80%% — %.1f%%", p.getId(), p.getWasteType() == null ? "?" : p.getWasteType(), newRatio);
                n.setMessage(msg);
                n.setCollectPointId(p.getId());
                n.setTourneeId(null);
                n.setLatitude(p.getLatitude());
                n.setLongitude(p.getLongitude());
                n.setRead(false);
                n.setTimestamp(System.currentTimeMillis());
                notificationService.save(n);
            }
        } catch (Exception ex) {
            System.err.println("Failed to create notification during randomize: " + ex.getMessage());
        }
    });

    return repo.saveAll(list);
}

    // Planifier = Randomize capacities (alias)
    public List<CollectPoint> planifier() {
        return randomizeCapacities();
    }

    // Mise à jour automatique du status
    public void updateStatus(CollectPoint p) {
        double cap = p.getCapacityLiters();
        double max = p.getMaxCapacityLiters();

        // Map capacity -> UI-friendly states used by the frontend
        // 0% => VIDE
        // 0% < ratio <= 50% => NORMAL
        // 50% < ratio <= 80% => PRESQUE_PLEIN
        // >80% || >= max => PLEIN

        double ratio = (cap / max) * 100.0;
        if (ratio <= 0) {
            p.setStatus("VIDE");
        } else if (ratio <= 50) {
            p.setStatus("NORMAL");
        } else if (ratio <= 80) {
            p.setStatus("PRESQUE_PLEIN");
        } else {
            p.setStatus("PLEIN");
        }
    }
}
