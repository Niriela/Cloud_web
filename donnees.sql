- Users (id, firebase_id, email, password, first_name, last_name, date, statuts_user_id, user_type_id) 
- Entreprise (id, name, address, phone, active)
- Statuts (id, libelle)
- Point (id, latitude, longitude)
- Type_signalement (id, libelle)
- Signalements (id, user_id, point_id, type_signalement_id, date, surface, budget, statuts_id, entreprise_id)
- Historique_signalements (id, signalements_id, statuts_id, date)
- Regles_gestion(id, libelle, valeur)
- Statuts_user (id, libelle)
- Historique_users(id, user_id, statuts_user_id, date)
- User_type (id, libelle)


INSERT INTO Type_signalement (id, libelle) VALUES
(1, 'En construction'),
(2, 'Accident'),
(3, 'Nid de poule'),
(4, 'Réparé'),
(5, 'Abîmé'),
(6, 'Alerte'),
(7, 'Zone rouge'),
(8, 'Fuite / eau'),
(9, 'EFT');

INSERT INTO User_type (id, libelle) VALUES
(1, 'Visiteur'),
(2, 'Utilisateur'),
(3, 'Manager');

INSERT INTO Users (id, email, password, first_name, last_name, date, statuts_user_id, user_type_id, failed_login_attempts) VALUES
(1, 'test@gmail.com', '123456', 'John', 'Doe', CURRENT_TIMESTAMP, 1, 3, 0);

INSERT INTO Statuts_user (id, libelle) VALUES
(1, 'Actif'),
(2, 'Bloque'),
(3, 'Banni');

INSERT INTO Statuts (id, libelle) VALUES
(1, 'Nouveau'),
(2, 'En cours'),
(3, 'Terminé'),
(4, 'Annulé');

INSERT INTO Regles_gestion (id, libelle, valeur) VALUES
(1, 'Duree_vie_session', '30'),
(2, 'Nombre_tentative_connexion', '3');


INSERT INTO users (
  firebase_id, email, password, first_name, last_name,
  date, updated_at, failed_login_attempts, statuts_user_id, user_type_id
) VALUES (
  'ABpjxL9FlmQjig7U1rXc2oBYQRv2',
  'manager@example.com',
  '123456',
  'Manager',
  'Admin',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  0,
  1,
  3
);

WITH canonical AS (
  SELECT
    id,
    ROUND(latitude::numeric, 6) AS lat,
    ROUND(longitude::numeric, 6) AS lon,
    MIN(id) OVER (
      PARTITION BY ROUND(latitude::numeric, 6), ROUND(longitude::numeric, 6)
    ) AS keep_id
  FROM point
  WHERE latitude IS NOT NULL AND longitude IS NOT NULL
)
UPDATE signalements s
SET point_id = c.keep_id
FROM canonical c
WHERE s.point_id = c.id AND c.id <> c.keep_id;

WITH canonical AS (
  SELECT
    id,
    MIN(id) OVER (
      PARTITION BY ROUND(latitude::numeric, 6), ROUND(longitude::numeric, 6)
    ) AS keep_id
  FROM point
  WHERE latitude IS NOT NULL AND longitude IS NOT NULL
)
DELETE FROM point p
USING canonical c
WHERE p.id = c.id AND c.id <> c.keep_id;

WITH canonical AS (
  SELECT
    id,
    MIN(id) OVER (
      PARTITION BY
        point_id, type_signalement_id, user_id, date,
        surface, budget, entreprise_id, statuts_id
    ) AS keep_id
  FROM signalements
)
DELETE FROM signalements s
USING canonical c
WHERE s.id = c.id AND c.id <> c.keep_id;
