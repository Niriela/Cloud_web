- Users (id, email, password, first_name, last_name, date, statuts_user_id, user_type_id) 
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