-- ============================================================================
--  Java · Accès aux données (SGBD-R) — schéma du fil rouge « quincaillerie »
--  Exécuté UNE SEULE FOIS, au premier démarrage du conteneur MariaDB.
--
--  Ce schéma est REPRIS TEL QUEL du module « Bases de données SQL avec MariaDB ».
--  C'est délibéré : les étudiants qui ont suivi ce module retrouvent un modèle
--  qu'ils ont eux-mêmes normalisé, et le travail devient « le mapper », non
--  « le comprendre ». Il n'est PAS modifié par les TP : Hibernate reste en
--  `validate`, et l'évolution du schéma passe par Flyway à partir du TP 14.
--
--  Deux bases identiques :
--    compta      → base de TRAVAIL, attaquée par les programmes Java.
--    compta_ref  → copie de référence intacte, pour remettre `compta` à neuf.
--
--  Points du modèle qui portent la pédagogie de ce module :
--    · `bon.delai` est NULLable          → getInt() vs getObject(…, Integer.class) (TP 02)
--    · `article.prix` est DECIMAL(7,2)   → BigDecimal, jamais double            (TP 02, 06)
--    · `article.ref` est UNIQUE          → clé métier pour equals/hashCode       (TP 06)
--    · `compo` a une PK COMPOSÉE + `qte` → entité d'association, pas @ManyToMany (TP 09)
--    · FK en RESTRICT et CASCADE mêlées  → cascade JPA ≠ ON DELETE du SGBD       (TP 08)
--    · le fournisseur 4 n'a aucun article, le bon 7 aucune ligne → cas limites
-- ============================================================================

-- ─────────────────────────────────────────────────────────── base compta
CREATE DATABASE IF NOT EXISTS compta
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_uca1400_ai_ci;

USE compta;


-- Ordre de suppression inverse de l'ordre de création : on retire d'abord
-- les tables qui référencent les autres.
DROP TABLE IF EXISTS compo;
DROP TABLE IF EXISTS bon;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS fournisseur;

-- ─────────────────────────────────────────────────────────────── fournisseur
CREATE TABLE fournisseur (
  id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nom   VARCHAR(60)  NOT NULL,
  ville VARCHAR(60)  NOT NULL,
  CONSTRAINT pk_fournisseur      PRIMARY KEY (id),
  CONSTRAINT uq_fournisseur_nom  UNIQUE (nom)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────────── article
-- `actif` permet de distinguer le catalogue courant de l'historique :
-- on ne supprime jamais un article référencé par une commande passée.
CREATE TABLE article (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  ref         VARCHAR(13)  NOT NULL,
  designation VARCHAR(255) NOT NULL,
  prix        DECIMAL(7,2) NOT NULL,
  actif       BOOLEAN      NOT NULL DEFAULT 1,
  id_fou      INT UNSIGNED NOT NULL,
  CONSTRAINT pk_article             PRIMARY KEY (id),
  CONSTRAINT uq_article_ref         UNIQUE (ref),
  CONSTRAINT ck_article_prix        CHECK (prix >= 0),
  CONSTRAINT fk_article_fournisseur FOREIGN KEY (id_fou) REFERENCES fournisseur (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────────────── bon
-- `delai` est NULLable : le délai de livraison n'est connu qu'à la réception.
-- NULL signifie donc « pas encore livré » — pas « livré en 0 jour ».
CREATE TABLE bon (
  id        INT UNSIGNED     NOT NULL AUTO_INCREMENT,
  numero    INT UNSIGNED     NOT NULL,
  date_cmde DATETIME         NOT NULL,
  delai     TINYINT UNSIGNED NULL,
  id_fou    INT UNSIGNED     NOT NULL,
  CONSTRAINT pk_bon             PRIMARY KEY (id),
  CONSTRAINT uq_bon_numero      UNIQUE (numero),
  CONSTRAINT fk_bon_fournisseur FOREIGN KEY (id_fou) REFERENCES fournisseur (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- ───────────────────────────────────────────────────────────────────── compo
-- Table d'association du n..n ARTICLE ↔ BON. C'est une association PURE : elle ne
-- porte que `qte` (l'attribut du couple) et n'a aucune identité propre — rien ne la
-- référence, elle n'a pas de cycle de vie. Son identité EST le couple, d'où la clé
-- primaire COMPOSÉE (id_art, id_bon), sans surrogate. Un `id` auto-incrémenté serait
-- une identité accidentelle ; on ne l'introduirait que le jour où une table enfant
-- référencerait une ligne de commande (retour, livraison partielle…).
-- ON DELETE CASCADE côté bon : une ligne de commande n'existe pas sans sa commande.
-- ON DELETE RESTRICT côté article : on n'efface pas un article qui a été commandé.
CREATE TABLE compo (
  id_art INT UNSIGNED NOT NULL,
  id_bon INT UNSIGNED NOT NULL,
  qte    INT UNSIGNED NOT NULL,
  -- La clé composée garantit à elle seule qu'un article ne figure jamais deux fois
  -- sur le même bon : aucun UNIQUE séparé n'est nécessaire.
  CONSTRAINT pk_compo         PRIMARY KEY (id_art, id_bon),
  CONSTRAINT ck_compo_qte     CHECK (qte > 0),
  CONSTRAINT fk_compo_article FOREIGN KEY (id_art) REFERENCES article (id) ON DELETE RESTRICT,
  CONSTRAINT fk_compo_bon     FOREIGN KEY (id_bon) REFERENCES bon (id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
--  Données
-- ============================================================================

-- Le fournisseur 4 ne fournit AUCUN article : c'est le cas de test du LEFT JOIN.
INSERT INTO fournisseur (id, nom, ville) VALUES
  (1, 'Française d''Imports',    'Nantes'),
  (2, 'FDM SA',                  'Lille'),
  (3, 'Dubois & Fils',           'Angers'),
  (4, 'Quincaillerie du Maine',  'Le Mans');

-- Deux paires d'articles partagent une désignation avec des fournisseurs
-- différents (3/4 et 8/12) : matière première de l'exercice d'auto-jointure.
-- L'article 11 est inactif : il ne doit pas apparaître dans le catalogue courant.
INSERT INTO article (id, ref, designation, prix, actif, id_fou) VALUES
  ( 1, 'A01', 'Perceuse P1',                                   74.99, 1, 1),
  ( 2, 'F01', 'Boulon laiton 4 x 40 mm (sachet de 10)',         2.25, 1, 2),
  ( 3, 'F02', 'Boulon laiton 5 x 40 mm (sachet de 10)',         4.45, 1, 2),
  ( 4, 'D01', 'Boulon laiton 5 x 40 mm (sachet de 10)',         4.40, 1, 3),
  ( 5, 'A02', 'Meuleuse 125 mm',                               37.85, 1, 1),
  ( 6, 'D03', 'Boulon acier zingué 4 x 40 mm (sachet de 10)',   1.80, 1, 3),
  ( 7, 'A03', 'Perceuse à colonne',                           185.25, 1, 1),
  ( 8, 'D04', 'Coffret mèches à bois',                         12.25, 1, 3),
  ( 9, 'F03', 'Coffret mèches plates',                          6.25, 1, 2),
  (10, 'F04', 'Fraises d''encastrement',                        8.14, 1, 2),
  (11, 'A04', 'Scie sauteuse 250 W',                           59.90, 0, 1),
  (12, 'F05', 'Coffret mèches à bois',                         13.10, 1, 2);

-- Le bon 7 ne contient AUCUNE ligne et son délai est NULL :
-- cas de test du NOT EXISTS et des fonctions d'agrégation face aux NULL.
INSERT INTO bon (id, numero, date_cmde, delai, id_fou) VALUES
  (1, 1, '2026-02-09 09:30:00',    3, 1),
  (2, 2, '2026-03-02 09:30:00',    5, 2),
  (3, 3, '2026-04-03 17:30:00',    2, 3),
  (4, 4, '2026-04-05 11:40:00',    2, 3),
  (5, 5, '2026-05-15 14:45:00',    7, 2),
  (6, 6, '2026-06-24 18:55:00',    0, 1),
  (7, 7, '2026-07-02 10:15:00', NULL, 2);

INSERT INTO compo (id_art, id_bon, qte) VALUES
  ( 1, 1,  3), ( 5, 1,  4), ( 7, 1,  1),
  ( 2, 2, 25), ( 3, 2, 15), ( 9, 2,  8), (10, 2, 11),
  ( 4, 3, 25), ( 6, 3, 40), ( 8, 3, 15),
  ( 4, 4, 10), ( 6, 4, 15), ( 8, 4,  8),
  ( 2, 5, 17), ( 3, 5, 13), (10, 5,  9),
  ( 1, 6,  2), ( 5, 6,  1), (12, 6,  6);

-- Statistiques à jour : sans cela, les estimations d'EXPLAIN sont fantaisistes.

ANALYZE TABLE fournisseur, article, bon, compo;

-- ─────────────────────────────────────────────────────────── base compta_ref
CREATE DATABASE IF NOT EXISTS compta_ref
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_uca1400_ai_ci;

USE compta_ref;


-- Ordre de suppression inverse de l'ordre de création : on retire d'abord
-- les tables qui référencent les autres.
DROP TABLE IF EXISTS compo;
DROP TABLE IF EXISTS bon;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS fournisseur;

-- ─────────────────────────────────────────────────────────────── fournisseur
CREATE TABLE fournisseur (
  id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nom   VARCHAR(60)  NOT NULL,
  ville VARCHAR(60)  NOT NULL,
  CONSTRAINT pk_fournisseur      PRIMARY KEY (id),
  CONSTRAINT uq_fournisseur_nom  UNIQUE (nom)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────────── article
-- `actif` permet de distinguer le catalogue courant de l'historique :
-- on ne supprime jamais un article référencé par une commande passée.
CREATE TABLE article (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  ref         VARCHAR(13)  NOT NULL,
  designation VARCHAR(255) NOT NULL,
  prix        DECIMAL(7,2) NOT NULL,
  actif       BOOLEAN      NOT NULL DEFAULT 1,
  id_fou      INT UNSIGNED NOT NULL,
  CONSTRAINT pk_article             PRIMARY KEY (id),
  CONSTRAINT uq_article_ref         UNIQUE (ref),
  CONSTRAINT ck_article_prix        CHECK (prix >= 0),
  CONSTRAINT fk_article_fournisseur FOREIGN KEY (id_fou) REFERENCES fournisseur (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────────────── bon
-- `delai` est NULLable : le délai de livraison n'est connu qu'à la réception.
-- NULL signifie donc « pas encore livré » — pas « livré en 0 jour ».
CREATE TABLE bon (
  id        INT UNSIGNED     NOT NULL AUTO_INCREMENT,
  numero    INT UNSIGNED     NOT NULL,
  date_cmde DATETIME         NOT NULL,
  delai     TINYINT UNSIGNED NULL,
  id_fou    INT UNSIGNED     NOT NULL,
  CONSTRAINT pk_bon             PRIMARY KEY (id),
  CONSTRAINT uq_bon_numero      UNIQUE (numero),
  CONSTRAINT fk_bon_fournisseur FOREIGN KEY (id_fou) REFERENCES fournisseur (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- ───────────────────────────────────────────────────────────────────── compo
-- Table d'association du n..n ARTICLE ↔ BON. C'est une association PURE : elle ne
-- porte que `qte` (l'attribut du couple) et n'a aucune identité propre — rien ne la
-- référence, elle n'a pas de cycle de vie. Son identité EST le couple, d'où la clé
-- primaire COMPOSÉE (id_art, id_bon), sans surrogate. Un `id` auto-incrémenté serait
-- une identité accidentelle ; on ne l'introduirait que le jour où une table enfant
-- référencerait une ligne de commande (retour, livraison partielle…).
-- ON DELETE CASCADE côté bon : une ligne de commande n'existe pas sans sa commande.
-- ON DELETE RESTRICT côté article : on n'efface pas un article qui a été commandé.
CREATE TABLE compo (
  id_art INT UNSIGNED NOT NULL,
  id_bon INT UNSIGNED NOT NULL,
  qte    INT UNSIGNED NOT NULL,
  -- La clé composée garantit à elle seule qu'un article ne figure jamais deux fois
  -- sur le même bon : aucun UNIQUE séparé n'est nécessaire.
  CONSTRAINT pk_compo         PRIMARY KEY (id_art, id_bon),
  CONSTRAINT ck_compo_qte     CHECK (qte > 0),
  CONSTRAINT fk_compo_article FOREIGN KEY (id_art) REFERENCES article (id) ON DELETE RESTRICT,
  CONSTRAINT fk_compo_bon     FOREIGN KEY (id_bon) REFERENCES bon (id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
--  Données
-- ============================================================================

-- Le fournisseur 4 ne fournit AUCUN article : c'est le cas de test du LEFT JOIN.
INSERT INTO fournisseur (id, nom, ville) VALUES
  (1, 'Française d''Imports',    'Nantes'),
  (2, 'FDM SA',                  'Lille'),
  (3, 'Dubois & Fils',           'Angers'),
  (4, 'Quincaillerie du Maine',  'Le Mans');

-- Deux paires d'articles partagent une désignation avec des fournisseurs
-- différents (3/4 et 8/12) : matière première de l'exercice d'auto-jointure.
-- L'article 11 est inactif : il ne doit pas apparaître dans le catalogue courant.
INSERT INTO article (id, ref, designation, prix, actif, id_fou) VALUES
  ( 1, 'A01', 'Perceuse P1',                                   74.99, 1, 1),
  ( 2, 'F01', 'Boulon laiton 4 x 40 mm (sachet de 10)',         2.25, 1, 2),
  ( 3, 'F02', 'Boulon laiton 5 x 40 mm (sachet de 10)',         4.45, 1, 2),
  ( 4, 'D01', 'Boulon laiton 5 x 40 mm (sachet de 10)',         4.40, 1, 3),
  ( 5, 'A02', 'Meuleuse 125 mm',                               37.85, 1, 1),
  ( 6, 'D03', 'Boulon acier zingué 4 x 40 mm (sachet de 10)',   1.80, 1, 3),
  ( 7, 'A03', 'Perceuse à colonne',                           185.25, 1, 1),
  ( 8, 'D04', 'Coffret mèches à bois',                         12.25, 1, 3),
  ( 9, 'F03', 'Coffret mèches plates',                          6.25, 1, 2),
  (10, 'F04', 'Fraises d''encastrement',                        8.14, 1, 2),
  (11, 'A04', 'Scie sauteuse 250 W',                           59.90, 0, 1),
  (12, 'F05', 'Coffret mèches à bois',                         13.10, 1, 2);

-- Le bon 7 ne contient AUCUNE ligne et son délai est NULL :
-- cas de test du NOT EXISTS et des fonctions d'agrégation face aux NULL.
INSERT INTO bon (id, numero, date_cmde, delai, id_fou) VALUES
  (1, 1, '2026-02-09 09:30:00',    3, 1),
  (2, 2, '2026-03-02 09:30:00',    5, 2),
  (3, 3, '2026-04-03 17:30:00',    2, 3),
  (4, 4, '2026-04-05 11:40:00',    2, 3),
  (5, 5, '2026-05-15 14:45:00',    7, 2),
  (6, 6, '2026-06-24 18:55:00',    0, 1),
  (7, 7, '2026-07-02 10:15:00', NULL, 2);

INSERT INTO compo (id_art, id_bon, qte) VALUES
  ( 1, 1,  3), ( 5, 1,  4), ( 7, 1,  1),
  ( 2, 2, 25), ( 3, 2, 15), ( 9, 2,  8), (10, 2, 11),
  ( 4, 3, 25), ( 6, 3, 40), ( 8, 3, 15),
  ( 4, 4, 10), ( 6, 4, 15), ( 8, 4,  8),
  ( 2, 5, 17), ( 3, 5, 13), (10, 5,  9),
  ( 1, 6,  2), ( 5, 6,  1), (12, 6,  6);

-- Statistiques à jour : sans cela, les estimations d'EXPLAIN sont fantaisistes.

ANALYZE TABLE fournisseur, article, bon, compo;

-- ============================================================================
--  EXTENSION PROPRE À CE MODULE
--
--  Le schéma ci-dessus est celui du module SQL, à l'identique. Trois colonnes
--  manquent pourtant pour couvrir des points de mapping incontournables — on les
--  ajoute ici, explicitement, plutôt que de faire semblant dans les slides :
--
--    fournisseur.rue, fournisseur.code_postal
--        → avec `ville`, ils forment un OBJET-VALEUR : @Embeddable Adresse (TP 06).
--          Sans eux, l'exemple n'aurait qu'une colonne et ne démontrerait rien.
--        → NULLables : les données existantes du module SQL ne les renseignent pas.
--
--    bon.statut
--        → mapping d'une énumération : @Enumerated(EnumType.STRING) (TP 06),
--          et démonstration du piège ORDINAL. VARCHAR(20), pas un entier.
--        → CHECK plutôt qu'ENUM MariaDB : lisible, portable, et l'ensemble des
--          valeurs reste vérifié par la base — pas seulement par le code Java.
--
--    article.version
--        → colonne de verrouillage optimiste : @Version (TP 13). Sans elle, la
--          mise à jour perdue ne peut pas être corrigée.
--
--  Ces trois ajouts sont appliqués aux DEUX bases pour qu'une réinitialisation
--  depuis compta_ref reste fidèle. Le TP 14 fera passer les évolutions
--  SUIVANTES par Flyway : à partir de là, plus une seule colonne à la main.
-- ============================================================================

-- ─────────────────────────────────── 1. types entiers : sortir du « UNSIGNED »
--
--  Le module SQL a typé toutes les clés en INT UNSIGNED — un choix défendable en SQL
--  pur : il double la plage utile et interdit les identifiants négatifs.
--
--  Java n'a pas d'entier non signé. Cette colonne devient donc immappable proprement :
--    · `Integer` couvre INT signé, soit la MOITIÉ de la plage — la borne haute déborde ;
--    · `Long` couvre la plage, mais Hibernate refuse de démarrer en `validate` :
--        « wrong column type in column [id] in table [article];
--          found [int unsigned (Types#INTEGER)], but expecting [bigint (Types#BIGINT)] »
--
--  Il n'y a pas de troisième option honnête : on ÉLARGIT le schéma. C'est une vraie
--  décision d'architecture, et elle illustre un principe du module — l'ORM ne fait pas
--  disparaître le défaut d'impédance, il vous force à le regarder. Le SGBD et le langage
--  n'ont pas le même système de types, et c'est au schéma de céder, pas au mapping de
--  mentir. `delai` (TINYINT UNSIGNED) et `version` (INT UNSIGNED) subissent le même sort,
--  pour la même raison.
--
--  Les clés étrangères doivent être retirées avant modification, puis rétablies :
--  MariaDB refuse de changer le type d'une colonne référencée.

-- Les contraintes de clé étrangère, dans l'ordre inverse des dépendances.
ALTER TABLE compta.compo   DROP FOREIGN KEY fk_compo_article, DROP FOREIGN KEY fk_compo_bon;
ALTER TABLE compta.bon     DROP FOREIGN KEY fk_bon_fournisseur;
ALTER TABLE compta.article DROP FOREIGN KEY fk_article_fournisseur;

ALTER TABLE compta.fournisseur MODIFY id     BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE compta.article     MODIFY id     BIGINT NOT NULL AUTO_INCREMENT,
                               MODIFY id_fou BIGINT NOT NULL;
ALTER TABLE compta.bon         MODIFY id     BIGINT NOT NULL AUTO_INCREMENT,
                               MODIFY id_fou BIGINT NOT NULL,
                               MODIFY numero INT    NOT NULL,
                               MODIFY delai  INT    NULL;
ALTER TABLE compta.compo       MODIFY id_art BIGINT NOT NULL,
                               MODIFY id_bon BIGINT NOT NULL,
                               MODIFY qte    INT    NOT NULL;

ALTER TABLE compta.article ADD CONSTRAINT fk_article_fournisseur
  FOREIGN KEY (id_fou) REFERENCES compta.fournisseur (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE compta.bon ADD CONSTRAINT fk_bon_fournisseur
  FOREIGN KEY (id_fou) REFERENCES compta.fournisseur (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE compta.compo ADD CONSTRAINT fk_compo_article
  FOREIGN KEY (id_art) REFERENCES compta.article (id) ON DELETE RESTRICT;
ALTER TABLE compta.compo ADD CONSTRAINT fk_compo_bon
  FOREIGN KEY (id_bon) REFERENCES compta.bon (id) ON DELETE CASCADE;

-- Idem sur la base de référence, pour qu'une réinitialisation reste fidèle.
ALTER TABLE compta_ref.compo   DROP FOREIGN KEY fk_compo_article, DROP FOREIGN KEY fk_compo_bon;
ALTER TABLE compta_ref.bon     DROP FOREIGN KEY fk_bon_fournisseur;
ALTER TABLE compta_ref.article DROP FOREIGN KEY fk_article_fournisseur;

ALTER TABLE compta_ref.fournisseur MODIFY id     BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE compta_ref.article     MODIFY id     BIGINT NOT NULL AUTO_INCREMENT,
                                   MODIFY id_fou BIGINT NOT NULL;
ALTER TABLE compta_ref.bon         MODIFY id     BIGINT NOT NULL AUTO_INCREMENT,
                                   MODIFY id_fou BIGINT NOT NULL,
                                   MODIFY numero INT    NOT NULL,
                                   MODIFY delai  INT    NULL;
ALTER TABLE compta_ref.compo       MODIFY id_art BIGINT NOT NULL,
                                   MODIFY id_bon BIGINT NOT NULL,
                                   MODIFY qte    INT    NOT NULL;

ALTER TABLE compta_ref.article ADD CONSTRAINT fk_article_fournisseur
  FOREIGN KEY (id_fou) REFERENCES compta_ref.fournisseur (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE compta_ref.bon ADD CONSTRAINT fk_bon_fournisseur
  FOREIGN KEY (id_fou) REFERENCES compta_ref.fournisseur (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE compta_ref.compo ADD CONSTRAINT fk_compo_article
  FOREIGN KEY (id_art) REFERENCES compta_ref.article (id) ON DELETE RESTRICT;
ALTER TABLE compta_ref.compo ADD CONSTRAINT fk_compo_bon
  FOREIGN KEY (id_bon) REFERENCES compta_ref.bon (id) ON DELETE CASCADE;

-- ─────────────────────────────────── 2. colonnes propres au mapping du module

ALTER TABLE compta.fournisseur
  ADD COLUMN rue         VARCHAR(120) NULL AFTER nom,
  ADD COLUMN code_postal VARCHAR(10)  NULL AFTER rue;
ALTER TABLE compta_ref.fournisseur
  ADD COLUMN rue         VARCHAR(120) NULL AFTER nom,
  ADD COLUMN code_postal VARCHAR(10)  NULL AFTER rue;

ALTER TABLE compta.bon
  ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON' AFTER date_cmde,
  ADD CONSTRAINT ck_bon_statut CHECK (statut IN ('BROUILLON', 'VALIDE', 'LIVRE', 'ANNULE'));
ALTER TABLE compta_ref.bon
  ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON' AFTER date_cmde,
  ADD CONSTRAINT ck_bon_statut CHECK (statut IN ('BROUILLON', 'VALIDE', 'LIVRE', 'ANNULE'));

-- INT signé : @Version se mappe sur un `int` Java, donc Types#INTEGER.
ALTER TABLE compta.article
  ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE compta_ref.article
  ADD COLUMN version INT NOT NULL DEFAULT 0;

-- Un jeu de valeurs cohérent : les bons livrés ont un délai renseigné, le bon 7
-- (sans ligne, délai NULL) reste au brouillon. Le fournisseur 4, qui ne fournit
-- rien, garde une adresse incomplète : c'est un cas limite utile au TP 06.
UPDATE compta.bon SET statut = 'LIVRE'  WHERE delai IS NOT NULL AND id <> 6;
UPDATE compta.bon SET statut = 'VALIDE' WHERE id = 6;
UPDATE compta_ref.bon SET statut = 'LIVRE'  WHERE delai IS NOT NULL AND id <> 6;
UPDATE compta_ref.bon SET statut = 'VALIDE' WHERE id = 6;

UPDATE compta.fournisseur SET rue = '12 rue des Forges',   code_postal = '44000' WHERE id = 1;
UPDATE compta.fournisseur SET rue = '5 avenue du Nord',    code_postal = '59000' WHERE id = 2;
UPDATE compta.fournisseur SET rue = '8 quai de la Maine',  code_postal = '49100' WHERE id = 3;
UPDATE compta_ref.fournisseur SET rue = '12 rue des Forges',  code_postal = '44000' WHERE id = 1;
UPDATE compta_ref.fournisseur SET rue = '5 avenue du Nord',   code_postal = '59000' WHERE id = 2;
UPDATE compta_ref.fournisseur SET rue = '8 quai de la Maine', code_postal = '49100' WHERE id = 3;

-- ─────────────────────────────────────────────────────────────────── droits
-- Le compte applicatif `app` ne doit recevoir QUE ce dont le code a besoin :
-- moindre privilège. Aucun DDL sur `compta` — le schéma n'est pas censé bouger
-- depuis l'application ; c'est Flyway, avec un autre compte, qui le fera au TP 14.
--
-- ATTENTION : l'entrypoint de l'image MariaDB a DÉJÀ accordé `ALL PRIVILEGES` sur
-- MARIADB_DATABASE au compte MARIADB_USER avant d'exécuter ce script. Un GRANT
-- restrictif s'AJOUTE aux privilèges existants, il ne les remplace pas — il faut
-- donc révoquer explicitement. Sans ce REVOKE, `app` peut faire du DDL, et
-- l'échec attendu du TP 06 (Hibernate en `validate` face à un schéma figé) ne se
-- produit plus. C'est exactement le genre de faux positif qui masque une faille.
REVOKE ALL PRIVILEGES ON compta.* FROM 'app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON compta.*    TO 'app'@'%';
GRANT SELECT                         ON compta_ref.* TO 'app'@'%';

-- Le TP 14 introduit Flyway : lui a besoin du DDL. Compte séparé, exprès.
CREATE USER IF NOT EXISTS 'migrator'@'%' IDENTIFIED BY 'migrator';
GRANT ALL PRIVILEGES ON compta.* TO 'migrator'@'%';

FLUSH PRIVILEGES;
