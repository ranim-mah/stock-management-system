DROP DATABASE IF EXISTS gestion_stock_isimm;

-- Création de la base de données
CREATE DATABASE gestion_stock_isimm;
USE gestion_stock_isimm;

-- Table Utilisateurs
CREATE TABLE IF NOT EXISTS users
(
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE  NOT NULL,
    password VARCHAR(100)        NOT NULL,
    email    VARCHAR(100) UNIQUE NOT NULL,
    role     ENUM ('admin', 'gestionnaire') DEFAULT 'gestionnaire'
) ENGINE = InnoDB;

-- Table Fournisseurs
CREATE TABLE IF NOT EXISTS Fournisseur
(
    id_fournisseur VARCHAR(20) PRIMARY KEY, -- VARCHAR conforme au code Java
    nom            VARCHAR(100) NOT NULL,
    contact        VARCHAR(20),
    adresse        VARCHAR(255),
    reputation     ENUM ('excellent', 'bon', 'moyen', 'mauvais') DEFAULT 'bon'
) ENGINE = InnoDB;

-- Table LocalStockage
CREATE TABLE IF NOT EXISTS LocalStockage
(
    code_local   VARCHAR(20) PRIMARY KEY,
    nom_local    VARCHAR(100)                                                          NOT NULL,
    type_local   ENUM ('MAGASIN', 'BUREAU', 'SALLE', 'AMPHI', 'BIBLIOTHEQUE', 'AUTRE') NOT NULL,
    batiment     VARCHAR(50),
    niveau       VARCHAR(20),
    capacite_max INT                                                                   NOT NULL,
    responsable  VARCHAR(100)
) ENGINE = InnoDB;

-- Table Article (avec seuil_alerte uniquement)
CREATE TABLE Article
(
    reference       VARCHAR(20)    NOT NULL,
    nom             VARCHAR(100)   NOT NULL,
    description     TEXT,
    type            VARCHAR(50)    NOT NULL,
    categorie       VARCHAR(50),
    quantite        INT     DEFAULT 0,
    prix_unitaire   DECIMAL(10, 2) NOT NULL,
    seuil_alerte    INT            NOT NULL,
    critique        BOOLEAN DEFAULT FALSE,
    date_peremption DATE,
    code_local      VARCHAR(20)    NOT NULL,
    id_fournisseur  VARCHAR(20),
    -- Clé primaire composée de reference et code_local
    PRIMARY KEY (reference, code_local),
    FOREIGN KEY (code_local) REFERENCES LocalStockage (code_local)
) ENGINE = InnoDB;

-- Table Service
CREATE TABLE IF NOT EXISTS Service
(
    id_service   INT AUTO_INCREMENT PRIMARY KEY,
    nom          VARCHAR(100) NOT NULL,
    responsable  VARCHAR(100),
    localisation VARCHAR(100),
    telephone    VARCHAR(20)
) ENGINE = InnoDB;

-- Table Commandes
CREATE TABLE IF NOT EXISTS Commande
(
    id_commande       INT AUTO_INCREMENT PRIMARY KEY,
    type_commande     ENUM ('INTERNE', 'EXTERNE') NOT NULL,
    reference_article VARCHAR(20)                 NOT NULL,
    quantite          INT                         NOT NULL,
    date_commande     DATETIME                               DEFAULT CURRENT_TIMESTAMP,
    statut            ENUM ('EN_ATTENTE', 'VALIDE', 'LIVRE') DEFAULT 'EN_ATTENTE',
    id_service        INT,
    id_fournisseur    VARCHAR(20), -- Type VARCHAR conforme à Fournisseur
    FOREIGN KEY (reference_article) REFERENCES Article (reference),
    FOREIGN KEY (id_service) REFERENCES Service (id_service),
    FOREIGN KEY (id_fournisseur) REFERENCES Fournisseur (id_fournisseur)
) ENGINE = InnoDB;

-- Table MouvementStock
CREATE TABLE IF NOT EXISTS MouvementStock
(
    id_mouvement      INT AUTO_INCREMENT PRIMARY KEY,
    type_mouvement    ENUM ('ENTREE', 'SORTIE', 'AJUSTEMENT') NOT NULL,
    reference_article VARCHAR(20)                             NOT NULL,
    quantite          INT                                     NOT NULL,
    prix_unitaire     DECIMAL(10, 2),
    code_local        VARCHAR(20)                             NOT NULL,
    date_mouvement    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reference_article) REFERENCES Article (reference),
    FOREIGN KEY (code_local) REFERENCES LocalStockage (code_local)
) ENGINE = InnoDB;

-- Vue pour les alertes (corrigée)
CREATE OR REPLACE VIEW VueAlertesStock AS
SELECT a.reference,
       a.nom,
       a.quantite,
       a.seuil_alerte, -- Utilisation du bon nom
       a.date_peremption,
       l.nom_local,
       CASE
           WHEN a.date_peremption < CURDATE() THEN 'PERIME'
           WHEN a.date_peremption <= CURDATE() + INTERVAL 7 DAY THEN 'PERIMITION_PROCHE'
           WHEN a.quantite = 0 THEN 'RUPTURE_STOCK'
           WHEN a.quantite <= a.seuil_alerte THEN 'RUPTURE_CRITIQUE' -- Plus de stock_minimal
           ELSE 'NORMAL'
           END AS type_alerte
FROM Article a
         JOIN LocalStockage l ON a.code_local = l.code_local;

-- Insertion des données de test (corrigées)


-- Créer la table LigneCommande
CREATE TABLE IF NOT EXISTS LigneCommande
(
    id_ligne          INT AUTO_INCREMENT PRIMARY KEY,
    id_commande       INT            NOT NULL,
    reference_article VARCHAR(20)    NOT NULL,
    quantite          INT            NOT NULL,
    prix_unitaire     DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (id_commande) REFERENCES Commande (id_commande),
    FOREIGN KEY (reference_article) REFERENCES Article (reference)
) ENGINE = InnoDB;


-- Ajouter après la table LigneCommande
CREATE TABLE IF NOT EXISTS LigneCommandeInterne
(
    id_ligne          INT AUTO_INCREMENT PRIMARY KEY,
    id_commande       INT         NOT NULL,
    reference_article VARCHAR(20) NOT NULL,
    quantite          INT         NOT NULL,
    FOREIGN KEY (id_commande) REFERENCES Commande (id_commande),
    FOREIGN KEY (reference_article) REFERENCES Article (reference)
) ENGINE = InnoDB;

ALTER TABLE LigneCommandeInterne
    ADD prix_unitaire DECIMAL(10, 2);

ALTER TABLE LigneCommandeInterne
    ADD code_local VARCHAR(20);



CREATE TABLE IF NOT EXISTS CommandeExterne
(
    id_commande    INT AUTO_INCREMENT PRIMARY KEY,
    id_fournisseur VARCHAR(20) NOT NULL,
    date_commande  DATETIME                               DEFAULT CURRENT_TIMESTAMP,
    montant_total  DECIMAL(10, 2),
    statut         ENUM ('EN_ATTENTE', 'VALIDE', 'LIVRE') DEFAULT 'EN_ATTENTE',
    FOREIGN KEY (id_fournisseur) REFERENCES Fournisseur (id_fournisseur)
) ENGINE = InnoDB;



ALTER TABLE Commande
    DROP FOREIGN KEY Commande_ibfk_1;
ALTER TABLE Commande
    ADD CONSTRAINT Commande_ibfk_1 FOREIGN KEY (reference_article)
        REFERENCES Article (reference) ON DELETE CASCADE;

-- Pour la table LigneCommande
ALTER TABLE LigneCommande
    DROP FOREIGN KEY LigneCommande_ibfk_2;
ALTER TABLE LigneCommande
    ADD CONSTRAINT LigneCommande_ibfk_2 FOREIGN KEY (reference_article)
        REFERENCES Article (reference) ON DELETE CASCADE;

-- Pour la table LigneCommandeInterne
ALTER TABLE LigneCommandeInterne
    DROP FOREIGN KEY LigneCommandeInterne_ibfk_2;
ALTER TABLE LigneCommandeInterne
    ADD CONSTRAINT LigneCommandeInterne_ibfk_2 FOREIGN KEY (reference_article)
        REFERENCES Article (reference) ON DELETE CASCADE;

-- Pour la table MouvementStock
ALTER TABLE MouvementStock
    DROP FOREIGN KEY MouvementStock_ibfk_1;
ALTER TABLE MouvementStock
    ADD CONSTRAINT MouvementStock_ibfk_1 FOREIGN KEY (reference_article)
        REFERENCES Article (reference) ON DELETE CASCADE;


INSERT INTO users (username, password, email, role)
VALUES ('admin1', 'password1', 'admin1@example.com', 'admin'),
       ('gestion1', 'password2', 'gestion1@example.com', 'gestionnaire'),
       ('admin2', 'password3', 'admin2@example.com', 'admin'),
       ('gestion2', 'password4', 'gestion2@example.com', 'gestionnaire'),
       ('admin3', 'password5', 'admin3@example.com', 'admin'),
       ('gestion3', 'password6', 'gestion3@example.com', 'gestionnaire'),
       ('admin4', 'password7', 'admin4@example.com', 'admin'),
       ('gestion4', 'password8', 'gestion4@example.com', 'gestionnaire'),
       ('admin5', 'password9', 'admin5@example.com', 'admin'),
       ('gestion5', 'password10', 'gestion5@example.com', 'gestionnaire');



INSERT INTO Fournisseur (id_fournisseur, nom, contact, adresse, reputation)
VALUES ('FR011', 'Fournisseur A', '2123456789', '1 Rue Alpha', 'bon'),
       ('FR012', 'Fournisseur B', '2123456790', '2 Rue Beta', 'excellent'),
       ('FR013', 'Fournisseur C', '2123456791', '3 Rue Gamma', 'moyen'),
       ('FR014', 'Fournisseur D', '2123456792', '4 Rue Delta', 'mauvais'),
       ('FR015', 'Fournisseur E', '2123456793', '5 Rue Epsilon', 'bon'),
       ('FR016', 'Fournisseur F', '2123456794', '6 Rue Zeta', 'excellent'),
       ('FR017', 'Fournisseur G', '2123456795', '7 Rue Eta', 'moyen'),
       ('FR018', 'Fournisseur H', '2123456796', '8 Rue Theta', 'bon'),
       ('FR019', 'Fournisseur I', '2123456797', '9 Rue Iota', 'excellent'),
       ('FR020', 'Fournisseur J', '2123456798', '10 Rue Kappa', 'mauvais');

INSERT INTO LocalStockage (code_local, nom_local, type_local, batiment, niveau, capacite_max, responsable)
VALUES ('LOC011', 'Magasin B', 'MAGASIN', 'Bâtiment G', 'RDC', 200, 'Mr. Alpha'),
       ('LOC012', 'Bureau 101', 'BUREAU', 'Bâtiment H', '1er étage', 5, 'Mme. Beta'),
       ('LOC013', 'Salle Réunion', 'SALLE', 'Bâtiment I', '2ème étage', 50, 'Dr. Gamma'),
       ('LOC014', 'Amphi Sud', 'AMPHI', 'Bâtiment J', 'RDC', 400, 'Mr. Delta'),
       ('LOC015', 'Bibliothèque Sud', 'BIBLIOTHEQUE', 'Bâtiment K', '1er étage', 120, 'Mme. Epsilon'),
       ('LOC016', 'Salle TP Physique', 'SALLE', 'Bâtiment L', 'Sous-sol', 35, 'Mr. Zeta'),
       ('LOC017', 'Bureau Accueil', 'BUREAU', 'Bâtiment M', 'RDC', 3, 'Mme. Eta'),
       ('LOC018', 'Salle Profs', 'SALLE', 'Bâtiment N', '2ème étage', 20, 'Dr. Theta'),
       ('LOC019', 'Local Maintenance', 'AUTRE', 'Bâtiment O', 'Sous-sol', 10, 'Technicien Iota'),
       ('LOC020', 'Magasin Sport', 'MAGASIN', 'Bâtiment P', 'RDC', 150, 'Mr. Kappa');


INSERT INTO Article (reference, nom, description, type, categorie, quantite, prix_unitaire, seuil_alerte, critique,
                     date_peremption, code_local)
VALUES ('ART011', 'Cahier', 'Cahier A4 96 pages', 'Consommable', 'Papeterie', 300, 1.20, 30, FALSE, NULL, 'LOC011'),
       ('ART012', 'Tablette', 'Samsung Tab A7', 'Durable', 'Informatique', 15, 250.00, 5, TRUE, NULL, 'LOC012'),
       ('ART013', 'Chaise', 'Chaise en plastique', 'Durable', 'Mobilier', 60, 15.00, 10, FALSE, NULL, 'LOC013'),
       ('ART014', 'Détergent', 'Nettoyant multi-surfaces', 'Consommable', 'Hygiène', 80, 3.20, 20, FALSE,
        CURDATE() + INTERVAL 365 DAY, 'LOC014'),
       ('ART015', 'Tapis de souris', 'Tapis gaming', 'Consommable', 'Accessoires', 90, 5.00, 10, FALSE, NULL, 'LOC015'),
       ('ART016', 'Câble Réseau', 'Câble RJ45 5m', 'Consommable', 'Informatique', 40, 7.00, 5, FALSE, NULL, 'LOC016'),
       ('ART017', 'Projecteur LED', 'Projecteur 4000 lumens', 'Durable', 'Audiovisuel', 7, 400.00, 2, TRUE, NULL,
        'LOC017'),
       ('ART018', 'Table Bureau', 'Table 120x60cm', 'Durable', 'Mobilier', 12, 100.00, 3, FALSE, NULL, 'LOC018'),
       ('ART019', 'Boîte d\'archives', 'Boîte 50cm', 'Consommable', 'Papeterie', 100, 2.00, 15, FALSE, NULL, 'LOC019'),
       ('ART020', 'Ventilateur', 'Ventilateur sur pied', 'Durable', 'Électroménager', 20, 45.00, 5, FALSE, NULL,
        'LOC020');


INSERT INTO Service (nom, responsable, localisation, telephone)
VALUES ('Finances', 'Mr. Alpha Finance', 'Bâtiment A', '99887766'),
       ('Informatique Avancée', 'Mme. Beta Tech', 'Bâtiment B', '88776655'),
       ('Direction', 'Dr. Gamma Leader', 'Bâtiment C', '77665544'),
       ('Maintenance', 'Mr. Delta Support', 'Bâtiment D', '66554433'),
       ('Accueil', 'Mme. Epsilon Reception', 'Bâtiment E', '55443322'),
       ('Communication', 'Mr. Zeta Media', 'Bâtiment F', '44332211'),
       ('Achats', 'Mme. Eta Buyer', 'Bâtiment G', '33221100'),
       ('Scolarité', 'Dr. Theta Admin', 'Bâtiment H', '22110099'),
       ('RH', 'Mr. Iota HR', 'Bâtiment I', '11009988'),
       ('Logistique Avancée', 'Mme. Kappa Stock', 'Bâtiment J', '00998877');

INSERT INTO Commande (type_commande, reference_article, quantite, id_service, statut)
VALUES ('INTERNE', 'ART011', 10, 1, 'VALIDE'),
       ('INTERNE', 'ART012', 5, 2, 'EN_ATTENTE'),
       ('INTERNE', 'ART013', 20, 3, 'LIVRE'),
       ('INTERNE', 'ART014', 15, 4, 'VALIDE'),
       ('INTERNE', 'ART015', 25, 5, 'EN_ATTENTE'),
       ('INTERNE', 'ART016', 10, 6, 'LIVRE'),
       ('INTERNE', 'ART017', 2, 7, 'VALIDE'),
       ('INTERNE', 'ART018', 5, 8, 'EN_ATTENTE'),
       ('INTERNE', 'ART019', 30, 9, 'LIVRE'),
       ('INTERNE', 'ART020', 7, 10, 'VALIDE');


INSERT INTO CommandeExterne (id_fournisseur, montant_total, statut)
VALUES ('FR011', 500.00, 'VALIDE'),
       ('FR012', 800.00, 'LIVRE'),
       ('FR013', 200.00, 'EN_ATTENTE'),
       ('FR014', 450.00, 'VALIDE'),
       ('FR015', 600.00, 'LIVRE'),
       ('FR016', 150.00, 'EN_ATTENTE'),
       ('FR017', 300.00, 'VALIDE'),
       ('FR018', 750.00, 'LIVRE'),
       ('FR019', 1000.00, 'EN_ATTENTE'),
       ('FR020', 900.00, 'VALIDE');



INSERT INTO LigneCommande (id_commande, reference_article, quantite, prix_unitaire)
VALUES (1, 'ART011', 10, 1.20),
       (2, 'ART012', 5, 250.00),
       (3, 'ART013', 20, 15.00),
       (4, 'ART014', 15, 3.20),
       (5, 'ART015', 25, 5.00),
       (6, 'ART016', 10, 7.00),
       (7, 'ART017', 2, 400.00),
       (8, 'ART018', 5, 100.00),
       (9, 'ART019', 30, 2.00),
       (10, 'ART020', 7, 45.00);


INSERT INTO LigneCommandeInterne (id_commande, reference_article, quantite)
VALUES (1, 'ART011', 10),
       (2, 'ART012', 5),
       (3, 'ART013', 20),
       (4, 'ART014', 15),
       (5, 'ART015', 25),
       (6, 'ART016', 10),
       (7, 'ART017', 2),
       (8, 'ART018', 5),
       (9, 'ART019', 30),
       (10, 'ART020', 7);


INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, prix_unitaire, code_local)
VALUES ('ENTREE', 'ART011', 100, 1.10, 'LOC011'),
       ('SORTIE', 'ART012', 5, 240.00, 'LOC012'),
       ('ENTREE', 'ART013', 50, 14.50, 'LOC013'),
       ('SORTIE', 'ART014', 10, 3.00, 'LOC014'),
       ('AJUSTEMENT', 'ART015', 2, 5.00, 'LOC015'),
       ('ENTREE', 'ART016', 20, 6.80, 'LOC016'),
       ('SORTIE', 'ART017', 1, 390.00, 'LOC017'),
       ('ENTREE', 'ART018', 5, 95.00, 'LOC018'),
       ('SORTIE', 'ART019', 10, 2.00, 'LOC019'),
       ('ENTREE', 'ART020', 3, 42.00, 'LOC020');


-- Ajout d'articles pour déclencher les alertes
INSERT INTO Article (reference, nom, description, type, categorie, quantite, prix_unitaire, seuil_alerte, critique,
                     date_peremption, code_local)
VALUES
-- Rupture de stock (quantité = 0)
('ART021', 'Stylos', 'Lot de 50 stylos bleus', 'Consommable', 'Papeterie', 0, 0.50, 10, FALSE, NULL, 'LOC011'),

-- Rupture critique (quantité <= seuil)
('ART022', 'Clés USB 64GB', 'Clé USB haute vitesse', 'Durable', 'Informatique', 3, 15.00, 5, TRUE, NULL, 'LOC012'),

-- Péremption proche (date dans 3 jours)
('ART023', 'Lait Stérilisé', 'Lait UHT 1L', 'Consommable', 'Alimentation', 20, 2.50, 15, FALSE,
 CURDATE() + INTERVAL 3 DAY, 'LOC014'),

-- Périmé (date dépassée)
('ART024', 'Yaourts', 'Yaourt nature', 'Consommable', 'Alimentation', 30, 1.20, 20, FALSE, '2023-12-01', 'LOC015');