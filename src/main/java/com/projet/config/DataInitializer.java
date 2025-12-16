package com.projet.config;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.projet.entity.CollectPoint;
import com.projet.entity.Employee;
import com.projet.entity.Notification;
import com.projet.entity.Vehicle;
import com.projet.repository.CollectPointRepository;
import com.projet.repository.EmployeeRepository;
import com.projet.repository.NotificationRepository;
import com.projet.repository.VehicleRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initCollectPoints(CollectPointRepository repository) {
        return args -> {
            if (repository.count() > 0) return; // éviter le doublon

            Random random = new Random();

            List<String> wasteTypes = List.of("plastique", "organique", "verre", "papier");

            double baseLat = 34.7406 + 0.05;   // plus en haut (nord)
            double baseLng = 10.7603 - 0.05;   // plus à gauche (ouest)


            for (int i = 0; i < 100; i++) {
                double latitude = baseLat + (random.nextDouble() - 0.5) * 0.1;
                double longitude = baseLng + (random.nextDouble() - 0.5) * 0.1;

                double maxCapacity = 500 + random.nextInt(1500); // 500 à 2000 L
                double currentCapacity = random.nextInt((int) maxCapacity + 1);
                double niveau = Math.round((currentCapacity / maxCapacity) * 100);


                String status;
                if (niveau < 25) status = "VIDE";
                else if (niveau < 60) status = "NORMAL";
                else if (niveau < 85) status = "PRESQUE_PLEIN";
                else status = "PLEIN";

                CollectPoint point = new CollectPoint();
                point.setLatitude(latitude);
                point.setLongitude(longitude);
                point.setWasteType(wasteTypes.get(random.nextInt(wasteTypes.size())));
                point.setMaxCapacityLiters(maxCapacity);
                point.setCapacityLiters(currentCapacity);
                point.setNiveau(niveau);
                point.setStatus(status);

                repository.save(point);
            }

            System.out.println("✅ 200 points de collecte ajoutés pour Sfax");
        };
    }

    @Bean
    CommandLineRunner initVehicles(VehicleRepository repository) {
        return args -> {
            if (repository.count() > 0) return;

            Random random = new Random();
            List<String> types = List.of("benne", "compacteur", "tri-selectif", "camion-grue");

            double baseLat = 34.7406 + 0.04;
            double baseLng = 10.7603 - 0.04;

            for (int i = 0; i < 50; i++) {
                Vehicle v = new Vehicle();
                v.setMatricule((100 + i) + " TN " + (1000 + i));
                v.setType(types.get(random.nextInt(types.size())));
                v.setCapacity(4000 + random.nextInt(9000));
                v.setAvailable(random.nextBoolean());
                v.setLatitude(baseLat + (random.nextDouble() - 0.5) * 0.05);
                v.setLongitude(baseLng + (random.nextDouble() - 0.5) * 0.05);
                repository.save(v);
            }

            System.out.println("✅ 60 véhicules ajoutés pour Sfax");
        };
    }

    @Bean
    CommandLineRunner initEmployees(EmployeeRepository repository) {
    return args -> {
        if (repository.count() > 0) return;

        Random random = new Random();

        List<String> skillsPool = List.of(
            "conducteur", "collecte", "tri", "maintenance"
        );

        List<String> prenoms = List.of(
            "Mohamed", "Ahmed", "Ali", "Sami", "Youssef", "Karim", "Houssem",
            "Aymen", "Walid", "Fares", "Amine", "Bilel", "Nizar", "Skander",
            "Slim", "Anis", "Rami", "Mehdi", "Maher", "Zied"
        );

        List<String> noms = List.of(
            "Ben Ali", "Ben Salah", "Trabelsi", "Bouazizi", "Chahed",
            "Kammoun", "Haddad", "Jaziri", "Ferjani", "Sassi",
            "Mansour", "Saidi", "Ayadi", "Gharbi", "Mnif",
            "Khelifi", "Bouslama", "Zouari", "Rebai", "Zribi"
        );

        for (int i = 0; i < 120; i++) {
            Employee e = new Employee();

            e.setPrenom(prenoms.get(random.nextInt(prenoms.size())));
            e.setName(noms.get(random.nextInt(noms.size())));
            e.setCin(String.format("%08d", 11100000 + i));

            // 1 à 3 compétences aléatoires
            int skillCount = 1 + random.nextInt(3);
            List<String> skills = skillsPool.stream()
                    .sorted((a, b) -> random.nextInt(3) - 1)
                    .limit(skillCount)
                    .toList();

            e.setSkills(skills);
            e.setAvailable(random.nextBoolean());

            repository.save(e);
        }

        System.out.println("✅ 120 employés tunisiens ajoutés");
    };
}


    @Bean
    CommandLineRunner initNotifications(NotificationRepository repository, CollectPointRepository collectPointRepository) {
        return args -> {
            if (repository.count() > 0) return;
            List<CollectPoint> points = collectPointRepository.findAll();
            if (points.isEmpty()) return;

            Random random = new Random();
            List<String> types = List.of("conteneur plein", "incident", "maintenance");

            for (int i = 0; i < 50; i++) {
                CollectPoint cp = points.get(random.nextInt(points.size()));
                Notification n = new Notification();
                n.setType(types.get(random.nextInt(types.size())));
                n.setMessage("Notif auto " + (i + 1));
                n.setCollectPointId(cp.getId());
                n.setTimestamp(System.currentTimeMillis() - random.nextInt(7 * 24 * 3600) * 1000L);
                n.setLatitude(cp.getLatitude());
                n.setLongitude(cp.getLongitude());
                n.setRead(random.nextBoolean());
                repository.save(n);
            }

            System.out.println("✅ 50 notifications ajoutées");
        };
    }
}
