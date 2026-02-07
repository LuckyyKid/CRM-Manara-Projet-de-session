CRM Manara – Module de gestion communautaire
Contexte du projet

Ce projet est réalisé dans le cadre du cours Applications Web 2 (420-G26-RO).
Il consiste à développer une application web transactionnelle asynchrone pour le Centre communautaire Manara, dont l’objectif est de moderniser la gestion des inscriptions, des activités et de la communication avec les parents.

Le projet s’inscrit dans un contexte réel et vise à démontrer la capacité à concevoir une solution web cohérente, sécurisée et évolutive, en respectant les bonnes pratiques vues au cours.

Objectifs du projet

Centraliser la gestion des inscriptions et des activités

Offrir une expérience utilisateur fluide et accessible aux parents

Simplifier l’administration et le suivi opérationnel

Fournir des tableaux de bord pour l’aide à la décision

Mettre en œuvre une architecture web moderne et asynchrone

Types d’utilisateurs
Parents / Tuteurs

Création et gestion de compte

Gestion des profils des enfants

Inscription aux activités

Consultation des plannings

Réception de notifications et de reçus

Administrateurs

Gestion des activités et des sessions

Suivi des inscriptions et des listes d’attente

Communication avec les parents

Accès aux tableaux de bord et statistiques

Animateurs

Consultation des plannings

Accès aux listes d’inscrits

Signalement de présence ou d’incidents (accès restreint)

Architecture générale

L’application repose sur une architecture multicouche respectant le pattern MVC.

Frontend (Web / Angular)
        ↓
Communication asynchrone (AJAX / HTTP sécurisé)
        ↓
API REST (Spring Boot)
        ↓
Services métier
        ↓
Persistance (JPA / Hibernate)
        ↓
Base de données (MySQL / PostgreSQL)

Technologies utilisées
Frontend

HTML5

CSS3

Bootstrap

JavaScript

jQuery

AJAX

Angular (Version 2)

Backend

Java

Spring MVC (Version 1)

Spring Boot (Version 2)

API REST

Sécurisation des accès (JWT / OAuth2 selon implémentation)

Base de données

MySQL ou PostgreSQL

JPA / Hibernate

Outils et méthodologie

GitLab (gestion de versions)

Méthodologie Agile avec livraisons progressives

Versions du projet
Version 1 – Application Spring MVC

Application web transactionnelle

Moteur de vues côté serveur

Architecture MVC complète

Implémentation de la logique métier et de la persistance

Communication asynchrone via AJAX

Version 2 – API REST Spring Boot et frontend Angular

Backend exposé sous forme d’API REST

Sécurisation des endpoints

Frontend Angular consommant l’API

Communication asynchrone complète

Séparation claire entre frontend et backend

Fonctionnalités principales (MVP)

Authentification et gestion des rôles

Gestion des comptes utilisateurs

Gestion des enfants

Gestion des activités et des sessions

Inscription aux activités

Notifications électroniques

Tableaux de bord avec indicateurs de base

Livrables attendus

Application web fonctionnelle

Code source versionné sur GitLab

Documentation technique

Guide utilisateur

Dossier compressé (.zip) pour la remise finale

Critères de succès

Stabilité et fiabilité de l’application

Respect des exigences fonctionnelles et techniques

Architecture claire et maintenable

Utilisation correcte de la communication asynchrone

Qualité du travail d’équipe et du code

Équipe

Projet réalisé par :

Ariel-Wilkins Saintil

Paul-Ryan Francois

Ahmed

Bafing Keita

Nom du dépôt GitLab :

CRM-Manara-Saintil-Francois-Ahmed-Keita

Remarque finale

Ce projet a été conçu dans une approche professionnelle, orientée vers la maintenabilité, la sécurité et l’évolutivité, tout en respectant les contraintes pédagogiques du cours Applications Web 2.