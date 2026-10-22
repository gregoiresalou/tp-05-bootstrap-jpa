## 2 — La première entité

**Fragment d'erreur :**
Schema validation: missing column [codePostal] in table [fournisseur]

**Pourquoi la correction se fait-elle chez le propriétaire ?**
La correction (`@AttributeOverride`) se fait dans l'entité propriétaire (`Fournisseur`) car la classe imbriquée (`Adresse`) doit rester réutilisable et indépendante du schéma SQL (une autre entité pourrait réutiliser `Adresse` avec des noms de colonnes différents).

**Requête affichée par logback :**
select f1_0.id,f1_0.rue,f1_0.code_postal,f1_0.ville,f1_0.nom from fournisseur f1_0 where f1_0.id=?

**estComplete() pour le fournisseur 4 :**
false