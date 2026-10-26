# Environnement de TP — Java · Accès aux données

Un conteneur MariaDB 12.3 LTS, un Adminer, deux bases. Rien d'autre à installer que Docker
et un JDK 25.

## Démarrage

```bash
cd env
docker compose up -d
docker compose ps            # les deux services doivent être "healthy" / "running"
```

- **Adminer** : <http://localhost:8080> — serveur `db`, utilisateur `root`, mot de passe `root`
- **CLI** : `docker compose exec db mariadb -uapp -papp compta`

Si le port 3306 est déjà occupé sur votre poste (XAMPP, WSL, un autre MySQL) :

```bash
DB_PORT=3307 docker compose up -d
```

…et adaptez `DB_URL` en conséquence.

## Les deux bases

| Base         | Rôle                                                                    |
| ------------ | ----------------------------------------------------------------------- |
| `compta`     | Base de **travail**. Vos programmes Java écrivent ici. Elle sera sale.   |
| `compta_ref` | Copie de **référence**, en lecture. Sert à remettre `compta` à neuf.     |

Le schéma est **fourni et figé** : c'est le même que celui du module SQL (quincaillerie).
Ce module ne consiste pas à concevoir un schéma mais à le **mapper**. Deux conséquences
directes sur vos TP :

- Hibernate reste en `validate` : il **vérifie** la concordance entités ↔ tables, il ne
  modifie jamais rien. Un écart doit se corriger côté Java, pas côté base.
- À partir du TP 14, le schéma passe sous Flyway et les migrations deviennent la seule
  façon légitime de le faire évoluer.

## Variables d'environnement attendues par le code Java

Aucun identifiant n'est écrit en dur, dès le TP 01. Déclarez-les dans la *run configuration*
de votre IDE (IntelliJ : `Run > Edit Configurations > Environment variables`) ou dans votre
shell :

```bash
export DB_URL='jdbc:mariadb://localhost:3306/compta?useServerPrepStmts=true&connectionTimeZone=SERVER'
export DB_USER='app'
export DB_PASSWORD='app'
```

> Sous IntelliJ, une variable d'environnement définie dans le terminal n'est **pas** vue par
> une exécution lancée depuis l'IDE. C'est l'erreur la plus fréquente du TP 01.

## Remettre la base de travail à neuf

Après un TP qui a laissé `compta` dans un état incohérent :

```bash
docker compose exec -T db sh -c \
  'mariadb -uroot -proot -e "DROP DATABASE IF EXISTS compta; CREATE DATABASE compta CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci; GRANT ALL ON compta.* TO app@\"%\";"'
docker compose exec -T db sh -c 'mariadb-dump -uroot -proot compta_ref | sed "1i USE compta;" | mariadb -uroot -proot'
```

Ou, radicalement — cela **détruit** le volume et rejoue les scripts d'initialisation :

```bash
docker compose down -v && docker compose up -d
```

## Voir le SQL réellement exécuté

Réflexe installé au chapitre 1 et indispensable à partir du chapitre 4. À activer le temps
d'une séance, **jamais** en production :

```sql
SET GLOBAL general_log = 1;
SET GLOBAL log_output  = 'TABLE';

SELECT event_time, argument
FROM   mysql.general_log
WHERE  command_type = 'Query'
ORDER  BY event_time DESC
LIMIT  30;

-- Remettre à zéro entre deux mesures :
TRUNCATE TABLE mysql.general_log;
```

Côté Java, la même information vient des logs Hibernate (`hibernate.show_sql`) et des
statistiques (`hibernate.generate_statistics`) — voir le TP 12.
