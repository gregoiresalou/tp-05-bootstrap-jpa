# Projet de départ du TP 05 — demo-data-access

L'état du projet à la fin du TP 04 : la couche JDBC complète du chapitre 1. Partez de ce dossier
si votre projet n'est pas terminé ou ne compile pas.

## Contenu

| Paquet          | Classes                                                                      |
| --------------- | ---------------------------------------------------------------------------- |
| `infra`         | `Env` (variables d'environnement), `Db` (pool HikariCP)                      |
| `domain`        | `Fournisseur`, `Adresse` (record), `Article`                                 |
| `dao`           | `FournisseurDao`, `ArticleDao`, `PersistenceFailure`                         |
| `dao.jdbc`      | `Tx` (frontière transactionnelle), `FournisseurDaoJdbc`, `ArticleDaoJdbc`, `BonDaoJdbc` |
| `app`           | les programmes d'essai des TP 01 à 04                                        |

`pom.xml` et `logback.xml` sont ceux du TP 01 : Hibernate y est déjà déclaré, et le SQL qu'il
génère s'affichera dès le TP 05.

## Mise en route

1. Copiez le contenu de ce dossier dans votre dépôt `demo-data-access`, à la place de vos
   sources. Gardez votre `NOTES.md`. Sans dépôt, suivez d'abord la mise en route du projet de
   départ du TP 01.
2. `mvn -q compile` passe.
3. Base démarrée (`env/`, `docker compose up -d`), `DB_URL`, `DB_USER` et `DB_PASSWORD` dans la
   configuration d'exécution :
   `mvn -q exec:java -Dexec.mainClass=org.sebsy.compta.app.EssaiFournisseurs` affiche les
   fournisseurs et laisse la base comme il l'a trouvée.
4. Un commit : « Reprendre le projet de départ du TP 05 ».
