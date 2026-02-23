-- Seed data for Sprint 1 (Manara)
-- Assumes tables already created by Hibernate
SET NAMES utf8mb4;

-- Clean existing data (optional)
DELETE FROM inscription;
DELETE FROM animation;
DELETE FROM enfant;
DELETE FROM parent;
DELETE FROM administrateurs;
DELETE FROM animateurs;
DELETE FROM users;

-- Password hash for "root" (BCrypt, 10 rounds)
-- $2b$10$yM6rbj82PB8PmO84RG0fsutELCVyr2PW7UIx7EgPs4J1eK8AabGKO

-- Users
INSERT INTO users (email, password, role, date_creation) VALUES
('admin@manara.local', '$2b$10$yM6rbj82PB8PmO84RG0fsutELCVyr2PW7UIx7EgPs4J1eK8AabGKO', 'ROLE_ADMIN', NOW()),
('animateur@manara.local', '$2b$10$yM6rbj82PB8PmO84RG0fsutELCVyr2PW7UIx7EgPs4J1eK8AabGKO', 'ROLE_ANIMATEUR', NOW()),
('parent@manara.local', '$2b$10$yM6rbj82PB8PmO84RG0fsutELCVyr2PW7UIx7EgPs4J1eK8AabGKO', 'ROLE_PARENT', NOW());

-- Administrateur
INSERT INTO administrateurs (nom, prenom, role, status, date_creation, last_login, user_id)
VALUES ('Admin', 'Manara', 'ROLE_ADMIN', 'ACTIF', NOW(), NULL,
        (SELECT id FROM users WHERE email='admin@manara.local'));

-- Animateur
INSERT INTO animateurs (nom, prenom, user_id)
VALUES ('Ali', 'Karim', (SELECT id FROM users WHERE email='animateur@manara.local'));

-- Parent
INSERT INTO parent (nom, prenom, adresse, user_id)
VALUES ('Dupont', 'Sara', '123 rue Centrale', (SELECT id FROM users WHERE email='parent@manara.local'));

-- Enfants
INSERT INTO enfant (nom, prenom, date_de_naissance, parent_id) VALUES
('Dupont', 'Lina', '2014-05-12', (SELECT id FROM parent WHERE nom='Dupont' AND prenom='Sara')),
('Dupont', 'Adam', '2012-09-21', (SELECT id FROM parent WHERE nom='Dupont' AND prenom='Sara'));

-- Activités
INSERT INTO activity (activy_name, description, age_min, age_max, capacity, date_creation, status, type_activity) VALUES
('Atelier Arts', 'Peinture et créativité', 6, 12, 12, CURDATE(), 'OUVERTE', 'ART'),
('Initiation Musique', 'Découverte des instruments', 8, 14, 10, CURDATE(), 'OUVERTE', 'MUSIQUE'),
('Sport collectif', 'Jeux d\'équipe et coopération', 10, 15, 14, CURDATE(), 'OUVERTE', 'SPORT');

-- Animations (sessions)
INSERT INTO animation (`start`, `end`, role, status, activity_id, animateur_id) VALUES
('2026-03-01 10:00:00', '2026-03-01 12:00:00', 'PRINCIPAL', 'ACTIF',
 (SELECT id FROM activity WHERE activy_name='Atelier Arts'),
 (SELECT id FROM animateurs WHERE nom='Ali' AND prenom='Karim')),
('2026-03-02 14:00:00', '2026-03-02 16:00:00', 'PRINCIPAL', 'ACTIF',
 (SELECT id FROM activity WHERE activy_name='Initiation Musique'),
 (SELECT id FROM animateurs WHERE nom='Ali' AND prenom='Karim')),
('2026-03-03 09:00:00', '2026-03-03 11:00:00', 'PRINCIPAL', 'ACTIF',
 (SELECT id FROM activity WHERE activy_name='Sport collectif'),
 (SELECT id FROM animateurs WHERE nom='Ali' AND prenom='Karim'));

-- Inscriptions
INSERT INTO inscription (status_inscription, animation_id, enfant_id) VALUES
('ACTIF', (SELECT id FROM animation ORDER BY id ASC LIMIT 1),
         (SELECT id FROM enfant WHERE prenom='Lina' LIMIT 1)),
('ACTIF', (SELECT id FROM animation ORDER BY id DESC LIMIT 1),
         (SELECT id FROM enfant WHERE prenom='Adam' LIMIT 1));
