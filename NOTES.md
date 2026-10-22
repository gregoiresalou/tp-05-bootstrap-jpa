## 2 — La première entité

**Fragment d'erreur :**
Schema validation: missing column [codePostal] in table [fournisseur]

**Pourquoi la correction se fait-elle chez le propriétaire ?**
La correction (`@AttributeOverride`) se fait dans l'entité propriétaire (`Fournisseur`) car la classe imbriquée (`Adresse`) doit rester réutilisable et indépendante du schéma SQL (une autre entité pourrait réutiliser `Adresse` avec des noms de colonnes différents).

**Requête affichée par logback :**
select f1_0.id,f1_0.rue,f1_0.code_postal,f1_0.ville,f1_0.nom from fournisseur f1_0 where f1_0.id=?

**estComplete() pour le fournisseur 4 :**
false

## 3 — Ce que validate refuse, et ce qu'il laisse passer

**1. Prédiction datée (22/10/2026 à 17h12) :**
Je prédis que Hibernate va bloquer le démarrage et lever une erreur, car le type Java `Integer` (32 bits) est trop petit pour contenir un `BIGINT` (64 bits) de la base de données, ce qui représente un risque d'incompatibilité de type.
**Constat :**
Contrairement à la prédiction, le démarrage a réussi. Validate a laissé passer la différence de taille entre Integer et BIGINT.

**Fragment Schema validation (id en String) :**
Schema validation: wrong column type encountered in column [id] in table [article]; found [bigint (Types#BIGINT)], but expecting [varchar(255) (Types#VARCHAR)]

**Qu'est-ce que ces deux résultats disent de ce que validate compare ?**
Validate vérifie uniquement la compatibilité globale des familles de types (il refuse de mapper une chaîne de caractères sur un numérique), mais il ne vérifie pas la précision ou la taille exacte (il laisse passer un Integer de 32 bits sur un BIGINT de 64 bits).

**Quel risque reste-t-il avec un Integer sur cette colonne ?**
Le risque est un dépassement de capacité (overflow). Si la base de données génère un identifiant supérieur à 2 147 483 647 (la valeur maximum d'un Integer en Java), l'application plantera silencieusement ou lèvera une exception au moment de lire l'enregistrement, car la valeur SQL ne rentrera pas dans la variable Java.

**Fragment Schema validation (table fournisseurs) :**
Schema validation: missing table [fournisseurs]

**L'application démarre-t-elle avec Action.UPDATE ?**
Non, le démarrage échoue.

**Première commande SQL et erreur MariaDB :**
Commande : alter table if exists fournisseur modify column rue varchar(255)
Erreur : Error: 1142-42000: ALTER command denied to user 'app'@'172.18.0.1' for table `compta`.`fournisseur`

**Qu'est-ce qui vous a protégé ?**
C'est la gestion des droits de la base de données. L'utilisateur MariaDB n'a pas les privilèges DDL (Data Definition Language, comme ALTER) pour modifier le schéma.

**Que se serait-il passé avec un compte qui a les droits ?**
Hibernate aurait modifié la structure de la table directement en base. C'est extrêmement dangereux en production (risque d'écraser le schéma voulu par les DBA ou de causer des pertes de données).