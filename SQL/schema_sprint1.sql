-- CRM Manara - Sprint 1
-- Script SQL (MySQL/MariaDB) base sur les entites JPA actuelles
-- Date: 2026-02-23

CREATE DATABASE IF NOT EXISTS `manara`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `manara`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `Inscription`;
DROP TABLE IF EXISTS `Animation`;
DROP TABLE IF EXISTS `enfant`;
DROP TABLE IF EXISTS `Administrateurs`;
DROP TABLE IF EXISTS `Animateurs`;
DROP TABLE IF EXISTS `Activity`;
DROP TABLE IF EXISTS `Parent`;
DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  `date_creation` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Parent` (
  `ID` BIGINT NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(255) NULL,
  `prenom` VARCHAR(255) NULL,
  `adresse` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_parent_user_id` (`user_id`),
  CONSTRAINT `fk_parent_user`
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Administrateurs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(255) NOT NULL,
  `prenom` VARCHAR(255) NOT NULL,
  `user_id` BIGINT NULL,
  `role` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `date_creation` DATETIME NOT NULL,
  `last_login` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_id` (`user_id`),
  CONSTRAINT `fk_admin_user`
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Animateurs` (
  `ID` BIGINT NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(255) NOT NULL,
  `prenom` VARCHAR(255) NOT NULL,
  `user_id` BIGINT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_animateur_user_id` (`user_id`),
  CONSTRAINT `fk_animateur_user`
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Activity` (
  `ID` BIGINT NOT NULL AUTO_INCREMENT,
  `ActivyName` VARCHAR(255) NULL,
  `Description` VARCHAR(255) NULL,
  `AgeMin` INT NULL,
  `AgeMax` INT NULL,
  `Capacity` INT NULL,
  `dateCreation` DATE NOT NULL,
  `status` VARCHAR(50) NULL,
  `typeActivity` VARCHAR(50) NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `enfant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(255) NOT NULL,
  `prenom` VARCHAR(255) NOT NULL,
  `date_de_naissance` DATE NOT NULL,
  `Parent_id` BIGINT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_enfant_parent_id` (`Parent_id`),
  CONSTRAINT `fk_enfant_parent`
    FOREIGN KEY (`Parent_id`) REFERENCES `Parent`(`ID`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Animation` (
  `ID` BIGINT NOT NULL AUTO_INCREMENT,
  `animateur_id` BIGINT NULL,
  `activity_id` BIGINT NULL,
  `Role` VARCHAR(50) NULL,
  `Status` VARCHAR(50) NULL,
  `Start` DATETIME NOT NULL,
  `End` DATETIME NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `idx_animation_animateur_id` (`animateur_id`),
  KEY `idx_animation_activity_id` (`activity_id`),
  CONSTRAINT `fk_animation_animateur`
    FOREIGN KEY (`animateur_id`) REFERENCES `Animateurs`(`ID`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_animation_activity`
    FOREIGN KEY (`activity_id`) REFERENCES `Activity`(`ID`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `Inscription` (
  `ID` BIGINT NOT NULL AUTO_INCREMENT,
  `StatusInscription` VARCHAR(50) NULL,
  `PresenceStatus` VARCHAR(50) NOT NULL,
  `IncidentNote` VARCHAR(1000) NULL,
  `enfant_id` BIGINT NOT NULL,
  `animation_id` BIGINT NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_inscription_enfant_animation` (`enfant_id`, `animation_id`),
  KEY `idx_inscription_animation_id` (`animation_id`),
  CONSTRAINT `fk_inscription_enfant`
    FOREIGN KEY (`enfant_id`) REFERENCES `enfant`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_inscription_animation`
    FOREIGN KEY (`animation_id`) REFERENCES `Animation`(`ID`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Donnees minimales optionnelles pour validation (decommenter au besoin)
-- INSERT INTO `Activity` (`ActivyName`, `Description`, `AgeMin`, `AgeMax`, `Capacity`, `dateCreation`, `status`, `typeActivity`)
-- VALUES ('Atelier dessin', 'Initiation au dessin', 6, 12, 15, CURRENT_DATE, 'OUVERTE', 'ART');
