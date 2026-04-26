-- =============================================================
-- DONNÉES DE TEST — CRM Manara
-- Idempotent : peut être exécuté plusieurs fois sans erreur.
-- =============================================================
-- Comptes créés :
--   admin@manara.test          / root   (administrateur)
--   sarah.martin@test.com      / root   (parent, abonnement ACTIF, 2 enfants couverts)
--   pierre.dupont@test.com     / root   (parent, sans abonnement)
--   karim.benali@test.com      / root   (animateur — Football, Guitare)
--   fatima.zahra@test.com      / root   (animatrice — Dessin, Tutorat)
-- =============================================================

DO $$
DECLARE
  -- User IDs
  v_admin_uid   BIGINT;
  v_sarah_uid   BIGINT;
  v_pierre_uid  BIGINT;
  v_karim_uid   BIGINT;
  v_fatima_uid  BIGINT;
  -- Profile IDs
  v_sarah_pid   BIGINT;
  v_pierre_pid  BIGINT;
  v_karim_aid   BIGINT;
  v_fatima_aid  BIGINT;
  -- Enfant IDs
  v_youssef_id  BIGINT;
  v_lina_id     BIGINT;
  v_adam_id     BIGINT;
  -- Activity IDs
  v_foot_act    BIGINT;
  v_dessin_act  BIGINT;
  v_guitare_act BIGINT;
  v_tutorat_act BIGINT;
  -- Animation IDs
  v_foot_anim1  BIGINT;
  v_foot_anim2  BIGINT;
  v_dessin_anim1 BIGINT;
  v_dessin_anim2 BIGINT;
  -- Subscription
  v_sarah_sub   BIGINT;
BEGIN

-- =============================================================
-- 1. USERS
-- =============================================================
INSERT INTO users (email, password, role, enabled)
VALUES ('admin@manara.test',
        '$2a$10$zHgUa8CK0SM/Ri8uKTm2GO7Bn/vpbBLl1JV2DhIUjiG/atG6oAejy',
        'ROLE_ADMIN', true)
ON CONFLICT (email) DO NOTHING;
SELECT id INTO v_admin_uid FROM users WHERE email = 'admin@manara.test';

INSERT INTO users (email, password, role, enabled)
VALUES ('sarah.martin@test.com',
        '$2a$10$zHgUa8CK0SM/Ri8uKTm2GO7Bn/vpbBLl1JV2DhIUjiG/atG6oAejy',
        'ROLE_PARENT', true)
ON CONFLICT (email) DO NOTHING;
SELECT id INTO v_sarah_uid FROM users WHERE email = 'sarah.martin@test.com';

INSERT INTO users (email, password, role, enabled)
VALUES ('pierre.dupont@test.com',
        '$2a$10$zHgUa8CK0SM/Ri8uKTm2GO7Bn/vpbBLl1JV2DhIUjiG/atG6oAejy',
        'ROLE_PARENT', true)
ON CONFLICT (email) DO NOTHING;
SELECT id INTO v_pierre_uid FROM users WHERE email = 'pierre.dupont@test.com';

INSERT INTO users (email, password, role, enabled)
VALUES ('karim.benali@test.com',
        '$2a$10$zHgUa8CK0SM/Ri8uKTm2GO7Bn/vpbBLl1JV2DhIUjiG/atG6oAejy',
        'ROLE_ANIMATEUR', true)
ON CONFLICT (email) DO NOTHING;
SELECT id INTO v_karim_uid FROM users WHERE email = 'karim.benali@test.com';

INSERT INTO users (email, password, role, enabled)
VALUES ('fatima.zahra@test.com',
        '$2a$10$zHgUa8CK0SM/Ri8uKTm2GO7Bn/vpbBLl1JV2DhIUjiG/atG6oAejy',
        'ROLE_ANIMATEUR', true)
ON CONFLICT (email) DO NOTHING;
SELECT id INTO v_fatima_uid FROM users WHERE email = 'fatima.zahra@test.com';

-- =============================================================
-- 2. ADMIN PROFILE
-- =============================================================
INSERT INTO administrateurs (nom, prenom, user_id, role, status, date_creation)
SELECT 'Admin', 'Manara', v_admin_uid, 'ROLE_ADMIN', 'ACTIF', NOW()
WHERE NOT EXISTS (SELECT 1 FROM administrateurs WHERE user_id = v_admin_uid);

-- =============================================================
-- 3. PARENT PROFILES
-- =============================================================
INSERT INTO parent (nom, prenom, adresse, user_id)
SELECT 'Martin', 'Sarah', '123 Rue des Lilas, Montreal', v_sarah_uid
WHERE NOT EXISTS (SELECT 1 FROM parent WHERE user_id = v_sarah_uid);
SELECT id INTO v_sarah_pid FROM parent WHERE user_id = v_sarah_uid;

INSERT INTO parent (nom, prenom, adresse, user_id)
SELECT 'Dupont', 'Pierre', '456 Avenue du Parc, Montreal', v_pierre_uid
WHERE NOT EXISTS (SELECT 1 FROM parent WHERE user_id = v_pierre_uid);
SELECT id INTO v_pierre_pid FROM parent WHERE user_id = v_pierre_uid;

-- =============================================================
-- 4. ANIMATEUR PROFILES
-- =============================================================
INSERT INTO animateurs (nom, prenom, user_id)
SELECT 'Benali', 'Karim', v_karim_uid
WHERE NOT EXISTS (SELECT 1 FROM animateurs WHERE user_id = v_karim_uid);
SELECT id INTO v_karim_aid FROM animateurs WHERE user_id = v_karim_uid;

INSERT INTO animateurs (nom, prenom, user_id)
SELECT 'Zahra', 'Fatima', v_fatima_uid
WHERE NOT EXISTS (SELECT 1 FROM animateurs WHERE user_id = v_fatima_uid);
SELECT id INTO v_fatima_aid FROM animateurs WHERE user_id = v_fatima_uid;

-- =============================================================
-- 5. ENFANTS
-- =============================================================
-- Sarah : Youssef (10 ans, actif) + Lina (7 ans, active)
INSERT INTO enfant (nom, prenom, date_de_naissance, active, parent_id)
SELECT 'Martin', 'Youssef', '2015-03-15', true, v_sarah_pid
WHERE NOT EXISTS (
  SELECT 1 FROM enfant WHERE prenom = 'Youssef' AND nom = 'Martin' AND parent_id = v_sarah_pid
);
SELECT id INTO v_youssef_id FROM enfant
WHERE prenom = 'Youssef' AND nom = 'Martin' AND parent_id = v_sarah_pid;

INSERT INTO enfant (nom, prenom, date_de_naissance, active, parent_id)
SELECT 'Martin', 'Lina', '2018-09-20', true, v_sarah_pid
WHERE NOT EXISTS (
  SELECT 1 FROM enfant WHERE prenom = 'Lina' AND nom = 'Martin' AND parent_id = v_sarah_pid
);
SELECT id INTO v_lina_id FROM enfant
WHERE prenom = 'Lina' AND nom = 'Martin' AND parent_id = v_sarah_pid;

-- Pierre : Adam (9 ans, actif)
INSERT INTO enfant (nom, prenom, date_de_naissance, active, parent_id)
SELECT 'Dupont', 'Adam', '2016-06-10', true, v_pierre_pid
WHERE NOT EXISTS (
  SELECT 1 FROM enfant WHERE prenom = 'Adam' AND nom = 'Dupont' AND parent_id = v_pierre_pid
);
SELECT id INTO v_adam_id FROM enfant
WHERE prenom = 'Adam' AND nom = 'Dupont' AND parent_id = v_pierre_pid;

-- =============================================================
-- 6. ACTIVITES
-- =============================================================
INSERT INTO activity (activy_name, description, age_min, age_max, capacity, status, type_activity, date_creation)
SELECT 'Football', 'Entrainement de football en equipe. Techniques de base, jeu collectif et esprit sportif.', 7, 16, 20, 'OUVERTE', 'SPORT', NOW()
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE activy_name = 'Football');
SELECT id INTO v_foot_act FROM activity WHERE activy_name = 'Football';

INSERT INTO activity (activy_name, description, age_min, age_max, capacity, status, type_activity, date_creation)
SELECT 'Dessin creatif', 'Initiation au dessin, aquarelle et arts plastiques dans une ambiance creative.', 5, 14, 15, 'OUVERTE', 'ART', NOW()
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE activy_name = 'Dessin creatif');
SELECT id INTO v_dessin_act FROM activity WHERE activy_name = 'Dessin creatif';

INSERT INTO activity (activy_name, description, age_min, age_max, capacity, status, type_activity, date_creation)
SELECT 'Guitare acoustique', 'Apprentissage de la guitare: accords de base, tablatures et repertoire pop.', 8, 18, 10, 'OUVERTE', 'MUSIQUE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE activy_name = 'Guitare acoustique');
SELECT id INTO v_guitare_act FROM activity WHERE activy_name = 'Guitare acoustique';

INSERT INTO activity (activy_name, description, age_min, age_max, capacity, status, type_activity, date_creation)
SELECT 'Tutorat mathematiques', 'Soutien scolaire en mathematiques adapte au niveau de chaque enfant.', 6, 16, 8, 'OUVERTE', 'TUTORAT', NOW()
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE activy_name = 'Tutorat mathematiques');
SELECT id INTO v_tutorat_act FROM activity WHERE activy_name = 'Tutorat mathematiques';

-- =============================================================
-- 7. ANIMATIONS (sessions a venir)
-- =============================================================
-- Football — samedis avec Karim
INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-05-03 09:00:00', '2026-05-03 11:00:00', v_karim_aid, v_foot_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-05-03 09:00:00' AND activity_id = v_foot_act
);
SELECT id INTO v_foot_anim1 FROM animation WHERE start_time = '2026-05-03 09:00:00' AND activity_id = v_foot_act;

INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-05-10 09:00:00', '2026-05-10 11:00:00', v_karim_aid, v_foot_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-05-10 09:00:00' AND activity_id = v_foot_act
);
SELECT id INTO v_foot_anim2 FROM animation WHERE start_time = '2026-05-10 09:00:00' AND activity_id = v_foot_act;

-- Dessin — mercredis avec Fatima
INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-04-30 14:00:00', '2026-04-30 16:00:00', v_fatima_aid, v_dessin_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-04-30 14:00:00' AND activity_id = v_dessin_act
);
SELECT id INTO v_dessin_anim1 FROM animation WHERE start_time = '2026-04-30 14:00:00' AND activity_id = v_dessin_act;

INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-05-07 14:00:00', '2026-05-07 16:00:00', v_fatima_aid, v_dessin_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-05-07 14:00:00' AND activity_id = v_dessin_act
);
SELECT id INTO v_dessin_anim2 FROM animation WHERE start_time = '2026-05-07 14:00:00' AND activity_id = v_dessin_act;

-- Guitare — vendredis avec Karim
INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-05-02 15:00:00', '2026-05-02 17:00:00', v_karim_aid, v_guitare_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-05-02 15:00:00' AND activity_id = v_guitare_act
);

INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-05-09 15:00:00', '2026-05-09 17:00:00', v_karim_aid, v_guitare_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-05-09 15:00:00' AND activity_id = v_guitare_act
);

-- Tutorat — mardis avec Fatima
INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-04-29 17:00:00', '2026-04-29 19:00:00', v_fatima_aid, v_tutorat_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-04-29 17:00:00' AND activity_id = v_tutorat_act
);

INSERT INTO animation (role, status, start_time, end_time, animateur_id, activity_id)
SELECT 'PRINCIPAL', 'ACTIF', '2026-05-06 17:00:00', '2026-05-06 19:00:00', v_fatima_aid, v_tutorat_act
WHERE NOT EXISTS (
  SELECT 1 FROM animation WHERE start_time = '2026-05-06 17:00:00' AND activity_id = v_tutorat_act
);

-- =============================================================
-- 8. INSCRIPTIONS
--    Youssef → Football 03/05 (APPROUVEE)
--    Lina    → Dessin   30/04 (EN_ATTENTE)
-- =============================================================
INSERT INTO inscription (status_inscription, presence_status, enfant_id, animation_id)
SELECT 'APPROUVEE', 'NON_SIGNEE', v_youssef_id, v_foot_anim1
WHERE NOT EXISTS (
  SELECT 1 FROM inscription WHERE enfant_id = v_youssef_id AND animation_id = v_foot_anim1
);

INSERT INTO inscription (status_inscription, presence_status, enfant_id, animation_id)
SELECT 'EN_ATTENTE', 'NON_SIGNEE', v_lina_id, v_dessin_anim1
WHERE NOT EXISTS (
  SELECT 1 FROM inscription WHERE enfant_id = v_lina_id AND animation_id = v_dessin_anim1
);

-- =============================================================
-- 9. ABONNEMENT STRIPE (Sarah — ACTIF, 2 enfants couverts)
-- =============================================================
INSERT INTO parent_subscription (
  parent_id, user_id, status, provider,
  covered_children_count, pending_covered_children_count,
  first_child_monthly_amount_cents, additional_child_monthly_amount_cents,
  current_period_start, current_period_end,
  cancel_at_period_end, created_at, updated_at
)
SELECT
  v_sarah_pid, v_sarah_uid, 'ACTIVE', 'STRIPE',
  2, 2,
  6000, 4000,
  NOW() - INTERVAL '10 days', NOW() + INTERVAL '20 days',
  false, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'
WHERE NOT EXISTS (SELECT 1 FROM parent_subscription WHERE parent_id = v_sarah_pid);
SELECT id INTO v_sarah_sub FROM parent_subscription WHERE parent_id = v_sarah_pid;

-- =============================================================
-- 10. ENFANTS COUVERTS (Youssef + Lina)
-- =============================================================
INSERT INTO parent_subscription_child (subscription_id, enfant_id, created_at)
SELECT v_sarah_sub, v_youssef_id, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM parent_subscription_child
  WHERE subscription_id = v_sarah_sub AND enfant_id = v_youssef_id
);

INSERT INTO parent_subscription_child (subscription_id, enfant_id, created_at)
SELECT v_sarah_sub, v_lina_id, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM parent_subscription_child
  WHERE subscription_id = v_sarah_sub AND enfant_id = v_lina_id
);

-- =============================================================
-- 11. CRÉNEAUX DE RENDEZ-VOUS (appointment slots)
-- =============================================================
-- Karim : lundi 28 avril
INSERT INTO appointment_slots (animateur_id, start_time, end_time, status, created_at)
SELECT v_karim_aid, '2026-04-28 10:00:00', '2026-04-28 11:00:00', 'AVAILABLE', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM appointment_slots WHERE animateur_id = v_karim_aid AND start_time = '2026-04-28 10:00:00'
);
INSERT INTO appointment_slots (animateur_id, start_time, end_time, status, created_at)
SELECT v_karim_aid, '2026-04-28 11:00:00', '2026-04-28 12:00:00', 'AVAILABLE', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM appointment_slots WHERE animateur_id = v_karim_aid AND start_time = '2026-04-28 11:00:00'
);
INSERT INTO appointment_slots (animateur_id, start_time, end_time, status, created_at)
SELECT v_karim_aid, '2026-05-05 10:00:00', '2026-05-05 11:00:00', 'AVAILABLE', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM appointment_slots WHERE animateur_id = v_karim_aid AND start_time = '2026-05-05 10:00:00'
);

-- Fatima : mardi 29 avril + mercredi 30 avril
INSERT INTO appointment_slots (animateur_id, start_time, end_time, status, created_at)
SELECT v_fatima_aid, '2026-04-29 13:00:00', '2026-04-29 14:00:00', 'AVAILABLE', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM appointment_slots WHERE animateur_id = v_fatima_aid AND start_time = '2026-04-29 13:00:00'
);
INSERT INTO appointment_slots (animateur_id, start_time, end_time, status, created_at)
SELECT v_fatima_aid, '2026-04-30 13:00:00', '2026-04-30 14:00:00', 'AVAILABLE', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM appointment_slots WHERE animateur_id = v_fatima_aid AND start_time = '2026-04-30 13:00:00'
);

-- =============================================================
RAISE NOTICE '=====================================================';
RAISE NOTICE 'Données de test insérées avec succès.';
RAISE NOTICE '-----------------------------------------------------';
RAISE NOTICE 'ADMIN        admin@manara.test          / root';
RAISE NOTICE 'PARENT 1     sarah.martin@test.com      / root';
RAISE NOTICE '             -> Abonnement ACTIF, 2 enfants couverts';
RAISE NOTICE '             -> Youssef inscrit Football (APPROUVEE)';
RAISE NOTICE '             -> Lina inscrite Dessin (EN_ATTENTE)';
RAISE NOTICE 'PARENT 2     pierre.dupont@test.com     / root';
RAISE NOTICE '             -> Aucun abonnement (teste la page billing)';
RAISE NOTICE 'ANIMATEUR 1  karim.benali@test.com      / root';
RAISE NOTICE 'ANIMATEUR 2  fatima.zahra@test.com      / root';
RAISE NOTICE '=====================================================';
END $$;
