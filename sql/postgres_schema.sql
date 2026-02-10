-- PostgreSQL schema for current backend entities.

CREATE TABLE IF NOT EXISTS statuts_user (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(255),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_type (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(255),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS regles_gestion (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(255),
    valeur VARCHAR(255),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS statuts (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(255),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS statuts_pourcentage (
    id BIGSERIAL PRIMARY KEY,
    statuts_id BIGINT NOT NULL UNIQUE,
    pourcentage INTEGER NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_statuts_pourcentage_statuts
        FOREIGN KEY (statuts_id) REFERENCES statuts(id),
    CONSTRAINT chk_statuts_pourcentage_range
        CHECK (pourcentage >= 0 AND pourcentage <= 100)
);

CREATE TABLE IF NOT EXISTS type_signalement (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(255),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS entreprise (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    address VARCHAR(255),
    phone VARCHAR(255),
    active BOOLEAN,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS point (
    id BIGSERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    firebase_id VARCHAR(255) UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    statuts_user_id BIGINT,
    user_type_id BIGINT,
    CONSTRAINT fk_users_statuts_user
        FOREIGN KEY (statuts_user_id) REFERENCES statuts_user(id),
    CONSTRAINT fk_users_user_type
        FOREIGN KEY (user_type_id) REFERENCES user_type(id)
);

CREATE TABLE IF NOT EXISTS signalements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    point_id BIGINT,
    type_signalement_id BIGINT,
    date TIMESTAMP,
    surface DOUBLE PRECISION,
    budget DOUBLE PRECISION,
    description TEXT,
    updated_at TIMESTAMP,
    statuts_id BIGINT,
    entreprise_id BIGINT,
    CONSTRAINT fk_signalements_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_signalements_point
        FOREIGN KEY (point_id) REFERENCES point(id),
    CONSTRAINT fk_signalements_type_signalement
        FOREIGN KEY (type_signalement_id) REFERENCES type_signalement(id),
    CONSTRAINT fk_signalements_statuts
        FOREIGN KEY (statuts_id) REFERENCES statuts(id),
    CONSTRAINT fk_signalements_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise(id)
);

CREATE TABLE IF NOT EXISTS photos (
    id BIGSERIAL PRIMARY KEY,
    signalements_id BIGINT,
    url TEXT,
    updated_at TIMESTAMP,
    CONSTRAINT fk_photos_signalements
        FOREIGN KEY (signalements_id) REFERENCES signalements(id)
);

CREATE INDEX IF NOT EXISTS idx_photos_signalements_id
    ON photos(signalements_id);

CREATE TABLE IF NOT EXISTS historique_signalements (
    id BIGSERIAL PRIMARY KEY,
    signalements_id BIGINT,
    statuts_id BIGINT,
    date TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_historique_signalements_signalements
        FOREIGN KEY (signalements_id) REFERENCES signalements(id),
    CONSTRAINT fk_historique_signalements_statuts
        FOREIGN KEY (statuts_id) REFERENCES statuts(id)
);

CREATE TABLE IF NOT EXISTS historique_users (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    statuts_user_id BIGINT,
    date TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_historique_users_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_historique_users_statuts_user
        FOREIGN KEY (statuts_user_id) REFERENCES statuts_user(id)
);
