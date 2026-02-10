-- ============================================
-- Script de mise à jour - Types de Réparations
-- Date: 2026-02-10
-- Description: Ajout des colonnes niveau et prix_par_m2
--              pour le calcul automatique du budget
-- Formule: budget = prix_par_m2 * niveau * surface_m2
-- ============================================

-- 1. Ajout de la colonne "niveau" (1-10)
ALTER TABLE type_signalement 
ADD COLUMN IF NOT EXISTS niveau INTEGER NOT NULL DEFAULT 1;

-- 2. Ajout de la colonne "prix_par_m2" (prix forfaitaire)
ALTER TABLE type_signalement 
ADD COLUMN IF NOT EXISTS prix_par_m2 DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- 3. Contrainte sur le niveau (entre 1 et 10)
ALTER TABLE type_signalement 
ADD CONSTRAINT chk_niveau CHECK (niveau >= 1 AND niveau <= 10);

-- 4. Contrainte sur le prix (positif ou zéro)
ALTER TABLE type_signalement 
ADD CONSTRAINT chk_prix_par_m2 CHECK (prix_par_m2 >= 0);

-- ============================================
-- Données d'exemple (optionnel - à adapter)
-- ============================================

-- Mise à jour des types existants avec des valeurs par défaut
UPDATE type_signalement SET niveau = 1, prix_par_m2 = 5000.00 WHERE niveau IS NULL OR niveau = 0;

-- Exemples de types de réparation avec niveaux et prix
-- Décommentez et adaptez selon vos besoins:

-- INSERT INTO type_signalement (libelle, niveau, prix_par_m2, updated_at) VALUES
-- ('Réparation légère', 1, 5000.00, NOW()),
-- ('Nettoyage', 2, 3000.00, NOW()),
-- ('Peinture', 3, 8000.00, NOW()),
-- ('Plomberie simple', 4, 12000.00, NOW()),
-- ('Électricité', 5, 15000.00, NOW()),
-- ('Maçonnerie légère', 6, 20000.00, NOW()),
-- ('Toiture partielle', 7, 35000.00, NOW()),
-- ('Rénovation moyenne', 8, 50000.00, NOW()),
-- ('Rénovation lourde', 9, 80000.00, NOW()),
-- ('Reconstruction', 10, 120000.00, NOW())
-- ON CONFLICT (libelle) DO UPDATE SET 
--     niveau = EXCLUDED.niveau,
--     prix_par_m2 = EXCLUDED.prix_par_m2,
--     updated_at = NOW();

-- ============================================
-- Vérification
-- ============================================
SELECT id, libelle, niveau, prix_par_m2, updated_at 
FROM type_signalement 
ORDER BY niveau ASC;
