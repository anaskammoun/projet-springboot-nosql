# Gestion Déchets – Backend (Spring Boot)

## Prérequis
- JDK 17+
- Maven 3.8+
- MongoDB en cours d'exécution (adapter l'URI dans `src/main/resources/application.properties` si besoin)

## Lancement
```bash
mvn spring-boot:run
```

## Structure (résumé)
- `src/main/java/com/projet` :
  - `controller` : endpoints REST
  - `service` : logique métier
  - `repository` : accès MongoDB
  - `dto` / `entity` : modèles échangés
- `src/main/resources/application.properties` : configuration MongoDB et serveur

## Notes
- Les indexes essentiels sont déjà annotés sur les entités (cin/matricule/status/type...).
- Les snapshots tournées contiennent les champs minimaux utilisés par le front (CIN employé, coordonnées point, véhicule).
