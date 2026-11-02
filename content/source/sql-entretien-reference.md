---
subject: sql
title: SQL & bases de données
category: data
position: 70
status: published
---

# SQL & bases de données : le document de référence pour les entretiens techniques

> Pour préparer un entretien : bases de données relationnelles, transactions, index et SQL pratique.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. BASES DE DONNÉES
═══════════════════════════════════════════

### Vue d'ensemble

La base de données est presque toujours le **goulot d'étranglement** d'une application backend, et la cause la plus fréquente des problèmes de performance. Deux raisons : c'est le seul composant réellement partagé (on peut multiplier les instances applicatives, pas si facilement la base), et c'est là que se paient les accès disque.

Le modèle relationnel repose sur une idée forte : les données sont structurées en tables reliées, et le moteur garantit leur **cohérence** via des contraintes et des transactions ACID. Le prix à payer est une certaine rigidité de schéma et une scalabilité horizontale difficile, c'est ce qui a motivé l'émergence du NoSQL, qui relâche certaines garanties pour gagner en souplesse et en distribution.

En Java, l'ORM (JPA/Hibernate) fait le pont entre objets et tables. Il fait gagner énormément de temps, mais il **masque le SQL généré** : et la majorité des problèmes de performance viennent de là : on écrit trois lignes de code qui déclenchent mille requêtes.

### Cours

**SQL vs NoSQL.** Relationnel (PostgreSQL, MySQL, Oracle) : schéma structuré, jointures, ACID, idéal pour des données fortement reliées. NoSQL : document (MongoDB, données semi-structurées), clé-valeur (Redis, cache, sessions, très rapide), colonne (Cassandra, écriture massive, distribution), graphe (Neo4j, relations complexes).

**ACID** : Atomicity (tout ou rien), Consistency (d'un état valide à un autre), Isolation (les transactions concurrentes ne se perturbent pas), Durability (un commit survit à un crash).

**Niveaux d'isolation** et anomalies évitées :
| Niveau | Dirty read | Non-repeatable read | Phantom |
|---|---|---|---|
| READ_UNCOMMITTED | possible | possible | possible |
| READ_COMMITTED (défaut usuel) | évité | possible | possible |
| REPEATABLE_READ | évité | évité | possible |
| SERIALIZABLE | évité | évité | évité |

Plus on monte, plus c'est sûr et plus le débit baisse.

**Index.** Un index (généralement un B-tree) accélère la lecture au prix d'écritures plus lentes et d'espace disque. À créer sur les colonnes des `WHERE`, `JOIN` et `ORDER BY` fréquents. Un **index composite** `(a, b)` sert pour `a` seul ou `a, b`, mais pas pour `b` seul, c'est la *leftmost prefix rule*. Un **index couvrant** contient toutes les colonnes de la requête, qui n'a alors même plus besoin de lire la table.

Attention : un index n'est pas gratuit. Sur-indexer une table à forte écriture dégrade les performances.

**Plan d'exécution** : `EXPLAIN` (ou `EXPLAIN ANALYZE`) montre comment le moteur compte exécuter la requête. Ce qu'on cherche : un *sequential scan* là où un index devrait servir, un mauvais ordre de jointure, ou une estimation de cardinalité très éloignée du réel, signe de statistiques obsolètes.

**JPA / Hibernate.** JPA est la spécification, Hibernate l'implémentation dominante.
- **Cycle de vie d'une entité** : transient (nouvelle, non gérée) → managed (attachée au contexte de persistance, les modifications sont détectées automatiquement) → detached (contexte fermé) → removed.
- Le **contexte de persistance** est un cache de premier niveau : dans une même transaction, deux `findById` du même id ne déclenchent qu'une requête.
- **Dirty checking** : modifier une entité managée suffit, Hibernate génère l'UPDATE au flush.
- **Fetch LAZY vs EAGER** : LAZY charge à la demande (défaut pour les collections), EAGER charge immédiatement (défaut pour `@ManyToOne`). L'EAGER par défaut est un piège classique : il charge des graphes entiers sans qu'on le demande.

**Le problème N+1** : charger N entités puis, pour chacune, une requête pour ses relations. Une boucle innocente déclenche des centaines de requêtes. Solutions : `JOIN FETCH` en JPQL, `@EntityGraph`, `@BatchSize` (chargement par lots), ou une projection qui ne récupère que le nécessaire.

**Verrouillage** : **optimiste** (`@Version`, on vérifie au commit que la version n'a pas changé, sinon exception ; idéal quand les conflits sont rares) ou **pessimiste** (`SELECT FOR UPDATE`, on verrouille dès la lecture ; réduit le débit et expose aux deadlocks).

**Pool de connexions** (HikariCP) : les connexions sont coûteuses à créer, on les réutilise. Dimensionnement délicat, trop petit, les threads attendent ; trop grand, multiplié par le nombre de pods, on dépasse la limite du serveur de base.

**Migrations** : Flyway (SQL versionné) ou Liquibase (changelog). Versionnées avec le code. Jamais `ddl-auto: update` en production.

**Normalisation** : éviter la redondance et les anomalies de mise à jour.
- **1NF** : valeurs atomiques, pas de groupes répétés (pas de colonne "telephones" contenant une liste).
- **2NF** : 1NF + chaque attribut non clé dépend de **toute** la clé (pas d'une partie d'une clé composite).
- **3NF** : 2NF + aucun attribut non clé ne dépend d'un autre attribut non clé (pas de dépendance transitive : la ville ne doit pas être stockée avec le code postal dans la table client si le code postal la détermine).
- **BCNF** : version plus stricte de la 3NF.
La **dénormalisation** volontaire (dupliquer une donnée pour éviter une jointure coûteuse, une table de reporting) est un compromis assumé pour la lecture, au prix de la cohérence à maintenir.

**Clés et contraintes** : clé primaire (unique, non nulle), clé **naturelle** (un numéro de sécurité sociale, risquée car elle peut changer) vs **technique**/*surrogate* (identifiant sans signification, préférable), clé candidate, clé étrangère (intégrité référentielle, avec `ON DELETE CASCADE` / `RESTRICT` / `SET NULL`), contraintes `UNIQUE`, `NOT NULL`, `CHECK`, `DEFAULT`. Les contraintes en base sont la dernière ligne de défense : la validation applicative ne suffit pas en concurrence (deux inserts simultanés du même email passent tous les deux la vérification applicative ; seul un index unique les départage).

**Types d'index** : **B-tree** (défaut, égalité et intervalles, tri), **hash** (égalité seule), sous PostgreSQL **GIN** (tableaux, JSONB, recherche plein texte), **GiST** (géométrie, intervalles), **BRIN** (très grosses tables triées naturellement, comme des logs par date), index **partiel** (`WHERE actif = true`), index sur **expression** (`lower(email)`), index **unique**. Index **clustered** (la table est physiquement rangée selon l'index, c'est le cas de la clé primaire en InnoDB/MySQL) vs non clustered. **Sélectivité** : un index sur une colonne à faible cardinalité (un booléen) est rarement utilisé, le moteur préfère un scan.

**Lire un plan d'exécution (PostgreSQL)** : *Seq Scan* (lecture complète), *Index Scan* (index puis table), *Index Only Scan* (l'index suffit, index couvrant), *Bitmap Index/Heap Scan* (combine plusieurs index, beaucoup de lignes), jointures *Nested Loop* (bonne avec peu de lignes et un index sur la table interne), *Hash Join* (grosses tables sans ordre), *Merge Join* (entrées triées). Comparer `rows` estimé et `actual rows` : un grand écart signale des statistiques obsolètes.

**MVCC** (Multi-Version Concurrency Control, PostgreSQL, Oracle, InnoDB) : chaque transaction voit un **instantané** cohérent ; les écritures créent de nouvelles versions de lignes au lieu d'écraser. Conséquence : **les lecteurs ne bloquent pas les écrivains, et inversement**. Les anciennes versions sont nettoyées plus tard (VACUUM en PostgreSQL ; une transaction très longue empêche ce nettoyage et fait gonfler les tables).

**Verrous et deadlocks en base** : verrous de ligne posés par `UPDATE`/`DELETE`/`SELECT FOR UPDATE`. Deux transactions qui verrouillent les mêmes lignes dans un ordre différent créent un **deadlock** ; la base le détecte et annule l'une d'elles (l'application doit pouvoir réessayer). Prévention : toujours modifier les lignes dans le même ordre, transactions courtes. `SELECT ... FOR UPDATE SKIP LOCKED` permet à plusieurs workers de se répartir une file de tâches en base sans se bloquer. Les niveaux d'isolation réels diffèrent selon le moteur : PostgreSQL est en READ COMMITTED par défaut et son REPEATABLE READ empêche aussi les lectures fantômes ; MySQL InnoDB est en REPEATABLE READ par défaut.

**Anomalies supplémentaires** : *lost update* (deux transactions lisent puis écrasent), *write skew* (deux transactions lisent un même invariant et écrivent chacune une ligne différente qui le viole, seul SERIALIZABLE l'empêche).

**Réplication, partitionnement, sharding** :
- **Réplication** : copies de la base. Primaire/réplicas (lectures réparties, latence de réplication donc lectures possiblement périmées), synchrone (aucune perte, plus lent) vs asynchrone. Sert la disponibilité et la lecture.
- **Partitionnement** : découper une grosse table en partitions **sur le même serveur** (par date, par plage) ; les requêtes ne lisent que les partitions concernées et on supprime une vieille partition instantanément.
- **Sharding** : répartir les données sur **plusieurs serveurs** selon une clé (client, région). Scalabilité en écriture, mais jointures et transactions inter-shards difficiles, et le choix de la clé est quasi définitif.
- Scalabilité **verticale** (plus grosse machine, simple, plafonnée) vs **horizontale** (plus de machines, complexe).

**Vues, procédures, triggers** : une **vue** est une requête nommée ; une **vue matérialisée** stocke le résultat (rapide en lecture, à rafraîchir). Les **procédures stockées** et **triggers** placent de la logique en base : performants mais difficiles à tester, versionner et déboguer ; à réserver à des besoins précis (audit, intégrité).

**NoSQL, choisir et modéliser** :
- **Document** (MongoDB) : on modélise selon les **requêtes** ; on **embarque** ce qui est lu ensemble (une commande et ses lignes), on **référence** ce qui est partagé ou volumineux. Index secondaires, transactions multi-documents possibles mais coûteuses.
- **Clé-valeur** (Redis) : structures (string, hash, list, set, sorted set pour des classements, stream), TTL, opérations atomiques ; persistance RDB (instantanés) et AOF (journal) ; haute disponibilité par Sentinel ou Cluster.
- **Colonnes larges** (Cassandra) : écriture massive, distribution sans maître, modélisation **une table par requête**, cohérence réglable par requête.
- **Recherche** (Elasticsearch/OpenSearch) : **index inversé** (mot → documents), pertinence, agrégations ; jamais comme source de vérité, on l'alimente depuis la base principale.
- **Graphe** (Neo4j) : relations à plusieurs niveaux (réseaux sociaux, recommandations).
- **Séries temporelles** (InfluxDB, TimescaleDB).
- PostgreSQL couvre beaucoup de besoins "NoSQL" avec `JSONB`, la recherche plein texte et des extensions (pgvector pour la recherche vectorielle).

**CDC** (*Change Data Capture*, Debezium) : lire le journal de transactions de la base (WAL, binlog) pour publier chaque modification comme un événement, sans modifier l'application. Base possible du pattern outbox.

**Sauvegarde** : sauvegardes complètes + journaux pour une restauration à un instant donné (*point-in-time recovery*), objectifs **RPO** (perte de données maximale acceptable) et **RTO** (durée maximale de restauration), et surtout des tests de restauration réguliers.

### En situation

- **"Cette requête est lente"** → EXPLAIN, index, N+1, volume ramené.
- **"Modification concurrente"** → verrouillage optimiste vs pessimiste.
- **"Migration sans coupure"** → changements additifs, compatibilité des deux versions.
- **"La base sature"** → optimiser, cache, réplicas, pool.

### Questions techniques

**Q : Que signifie ACID ?** R : Atomicity, Consistency, Isolation, Durability.

**Q : Dirty read, non-repeatable read, phantom ?** R : Lire du non commité / relire et obtenir autre chose / voir apparaître de nouvelles lignes.

**Q : Cycle de vie d'une entité JPA ?** R : Transient → managed → detached → removed.

**Q : C'est quoi le dirty checking ?** R : Hibernate détecte les modifications d'une entité managée et génère l'UPDATE au flush.

**Q : Fetch LAZY vs EAGER ?** R : À la demande vs immédiatement. EAGER par défaut sur @ManyToOne est un piège.

**Q : C'est quoi le N+1 et comment le résoudre ?** R : 1 requête + N requêtes pour les relations. Résolu par JOIN FETCH, @EntityGraph, @BatchSize.

**Q : Leftmost prefix rule ?** R : Un index `(a,b)` sert pour `a` ou `a,b`, jamais pour `b` seul.

**Q : C'est quoi un index couvrant ?** R : Un index contenant toutes les colonnes de la requête, qui évite de lire la table.

**Q : Optimiste vs pessimiste ?** R : Vérification de version au commit vs verrou dès la lecture.

**Q : Pourquoi un pool de connexions ?** R : Créer une connexion est coûteux ; on les réutilise. À dimensionner selon la capacité de la base.

**Q : Les trois premières formes normales ?** R : 1NF valeurs atomiques ; 2NF dépendance de toute la clé ; 3NF pas de dépendance transitive entre attributs non clés.

**Q : Pourquoi dénormaliser ?** R : Pour accélérer les lectures en évitant des jointures, au prix de la redondance.

**Q : Clé naturelle vs technique ?** R : Une donnée métier qui identifie (peut changer) vs un identifiant sans signification (stable).

**Q : Pourquoi une contrainte unique en base même si l'application vérifie ?** R : Deux requêtes concurrentes peuvent passer la vérification applicative ; seule la base garantit l'unicité.

**Q : Index B-tree vs hash ?** R : Égalité, intervalles et tri vs égalité uniquement.

**Q : C'est quoi un index partiel ?** R : Un index qui ne couvre que les lignes vérifiant une condition.

**Q : Pourquoi un index sur un booléen est-il souvent inutile ?** R : Sa sélectivité est trop faible ; le moteur préfère un scan.

**Q : Index clustered ?** R : La table est physiquement ordonnée selon cet index (une seule par table).

**Q : Seq Scan vs Index Scan vs Index Only Scan ?** R : Lecture de toute la table / index puis table / index seul.

**Q : C'est quoi le MVCC ?** R : Chaque transaction lit un instantané ; les écritures créent des versions, donc lecteurs et écrivains ne se bloquent pas.

**Q : Pourquoi une transaction très longue est-elle nocive en PostgreSQL ?** R : Elle retient des verrous et empêche le VACUUM de nettoyer les anciennes versions.

**Q : Comment la base gère-t-elle un deadlock ?** R : Elle le détecte et annule une des transactions, qu'il faut pouvoir réessayer.

**Q : À quoi sert SKIP LOCKED ?** R : À ignorer les lignes déjà verrouillées, pour que plusieurs workers consomment une file en base sans se bloquer.

**Q : Niveau d'isolation par défaut de PostgreSQL et MySQL ?** R : READ COMMITTED pour PostgreSQL, REPEATABLE READ pour MySQL InnoDB.

**Q : C'est quoi un write skew ?** R : Deux transactions qui violent ensemble un invariant en modifiant chacune une ligne différente ; seul SERIALIZABLE l'empêche.

**Q : Réplication vs partitionnement vs sharding ?** R : Copies des données / découpage d'une table sur un serveur / répartition des données sur plusieurs serveurs.

**Q : Vue vs vue matérialisée ?** R : Requête nommée recalculée à chaque appel vs résultat stocké à rafraîchir.

**Q : Embarquer ou référencer en MongoDB ?** R : Embarquer ce qui est lu ensemble et borné ; référencer ce qui est partagé, volumineux ou croissant.

**Q : Les structures de données de Redis ?** R : String, hash, list, set, sorted set, stream, bitmap, HyperLogLog.

**Q : RDB vs AOF en Redis ?** R : Instantanés périodiques vs journal de toutes les écritures (plus durable).

**Q : C'est quoi un index inversé ?** R : Une structure qui associe chaque terme aux documents qui le contiennent (Elasticsearch).

**Q : C'est quoi le CDC ?** R : Capturer les modifications depuis le journal de la base pour les publier en événements (Debezium).

**Q : RPO vs RTO ?** R : Perte de données maximale acceptable vs durée maximale d'indisponibilité.

### Questions de situation

**Q : "Une requête qui prenait 50 ms en prend 3 secondes."**
R : "Je procéderais par élimination.
D'abord : **qu'est-ce qui a changé ?** Volume de données, déploiement modifiant la requête, index supprimé ? Une dégradation soudaine a presque toujours une cause identifiable.
Ensuite le **plan d'exécution** avec `EXPLAIN ANALYZE`. Je cherche un scan séquentiel là où un index devrait servir, un mauvais ordre de jointure, ou une estimation de cardinalité très fausse, auquel cas les statistiques sont obsolètes et un `ANALYZE` peut suffire.
Le scénario le plus fréquent : la table a grossi, et une requête qui scannait mille lignes en scanne un million. L'index qui n'était pas nécessaire à petite échelle le devient.
Je vérifierais aussi côté application qu'il ne s'agit pas d'un **N+1** : une lenteur apparente peut cacher des centaines de requêtes. Les logs SQL ou une trace le montrent immédiatement.
Et si la requête est correcte mais ramène trop de données, la réponse est la pagination ou une projection ciblée."

**Q : "Deux utilisateurs modifient la même commande, l'un écrase l'autre."**
R : "C'est un *lost update*. Deux stratégies selon la fréquence des conflits.
Conflits **rares** : le cas courant : **verrouillage optimiste** avec un champ `@Version`. Au commit, Hibernate vérifie que la version en base est celle qui avait été lue ; sinon il lève une `OptimisticLockException`. On n'a bloqué personne et on détecte au bon moment. Côté API, je remonterais un **409 Conflict** en demandant de recharger, c'est aussi la meilleure expérience, l'utilisateur voit les modifications de l'autre avant de refaire les siennes.
Conflits **fréquents** et rejeu coûteux : **verrouillage pessimiste** avec `SELECT FOR UPDATE`. Mais ça bloque les autres transactions, réduit le débit et expose aux deadlocks, donc en dernier recours.
Dans les deux cas, je garderais la transaction la plus courte possible : jamais d'appel réseau à l'intérieur."

**Q : "Comment faire une migration de schéma sans interruption ?"**
R : "Le point clé : pendant un déploiement progressif, **l'ancienne et la nouvelle version du code tournent simultanément**. Le schéma doit être compatible avec les deux.
Donc des migrations **progressives en plusieurs déploiements**. Pour renommer une colonne, jamais de `RENAME` direct : d'abord ajouter la nouvelle et écrire dans les deux ; puis déployer le code qui lit la nouvelle et migrer les données existantes ; enfin, quand plus aucune version n'utilise l'ancienne, la supprimer.
Règle générale : les changements **additifs** sont sûrs (colonne nullable, index, table). Les changements **destructifs** doivent être différés.
J'utiliserais Flyway ou Liquibase versionné avec le code, et jamais `ddl-auto: update` en production.
Attention enfin aux migrations **lourdes** : créer un index sur une grosse table peut la verrouiller longtemps. Sur PostgreSQL, `CREATE INDEX CONCURRENTLY` évite ça."

**Q : "La base sature sous la charge."**
R : "Du moins au plus coûteux.
**Optimiser l'existant** : identifier les requêtes les plus consommatrices via les statistiques de la base, corriger les N+1, ajouter les index manquants, éliminer ce qui ramène trop de données. C'est presque toujours là que se trouve le gros du gain.
**Réduire le nombre d'appels** : du cache applicatif ou distribué sur les données lues souvent et modifiées rarement, une donnée servie depuis Redis n'atteint jamais la base.
**Vérifier la configuration** : le pool de connexions multiplié par le nombre de pods peut dépasser la limite du serveur.
**Répartir** : des réplicas en lecture pour décharger le primaire, efficace quand le trafic est majoritairement en lecture, en acceptant un léger décalage de réplication.
Et en dernier recours seulement, **partitionner ou sharder** : structurant, difficile à défaire, et complique toutes les requêtes transverses.
Le fil directeur : optimiser avant de scaler, sinon on paie plus cher pour le même problème."

**Q : "Deux utilisateurs s'inscrivent au même instant avec le même email et les deux comptes sont créés."**
R : "La vérification 'l'email existe-t-il ?' suivie de l'insertion est un **check-then-act** : deux transactions concurrentes passent la vérification avant que l'une ou l'autre n'insère. Aucun niveau d'isolation usuel ne l'empêche en READ COMMITTED.
La correction est une **contrainte unique** en base sur l'email (normalisé en minuscules, éventuellement via un index sur `lower(email)`). L'application traduit la violation de contrainte en réponse 409. Je garderais la vérification applicative pour un message d'erreur agréable dans le cas courant, mais c'est la base qui garantit l'invariant."

**Q : "Une table de 2 milliards de lignes d'événements devient ingérable."**
R : "Je regarderais d'abord les **usages** : quelles requêtes, sur quelle période ? Le plus souvent on interroge les données récentes et on garde l'historique pour l'audit.
Dans ce cas, un **partitionnement par date** (mensuel par exemple) : les requêtes sur la période récente ne lisent qu'une partition, les index restent petits, et purger ou archiver une période revient à détacher une partition, instantanément, au lieu d'un `DELETE` massif.
J'archiverais les partitions anciennes vers un stockage moins cher (stockage objet en Parquet, entrepôt analytique). Pour l'analytique lourde, une base orientée colonnes ou un entrepôt dédié serait plus adaptée qu'une base transactionnelle. Le sharding n'interviendrait que si une seule machine ne suffit plus en écriture."

═══════════════════════════════════════════
# 2. SQL PRATIQUE
═══════════════════════════════════════════

### Vue d'ensemble

Même sur un poste où l'ORM fait l'essentiel, savoir écrire et lire du SQL reste attendu, pour comprendre ce qu'Hibernate génère, pour diagnostiquer une lenteur, et parce que certains traitements (rapports, agrégations, migrations) s'écrivent bien mieux directement en SQL.

L'ordre d'exécution logique d'une requête explique la plupart des erreurs courantes : `FROM` → `JOIN` → `WHERE` → `GROUP BY` → `HAVING` → `SELECT` → `ORDER BY` → `LIMIT`. C'est parce que `WHERE` s'exécute avant `GROUP BY` qu'on ne peut pas y filtrer sur un agrégat, il faut `HAVING`. Et c'est parce que `SELECT` vient après `GROUP BY` qu'on ne peut sélectionner qu'une colonne groupée ou agrégée.

### Cours

**Jointures :**
```sql
INNER JOIN   -- uniquement les lignes correspondantes des deux côtés
LEFT JOIN    -- toutes les lignes de gauche, NULL à droite si pas de correspondance
RIGHT JOIN   -- l'inverse
FULL JOIN    -- tout des deux côtés
CROSS JOIN   -- produit cartésien
```
Astuce fréquente en entretien : trouver les lignes **sans correspondance** se fait par `LEFT JOIN ... WHERE b.id IS NULL`.

**Agrégation :**
```sql
SELECT city, COUNT(*) AS nb, AVG(age) AS moyenne
FROM users
WHERE active = true          -- filtre les lignes AVANT groupement
GROUP BY city
HAVING COUNT(*) > 10         -- filtre les groupes APRÈS agrégation
ORDER BY nb DESC
LIMIT 10;
```
Fonctions : `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`. Attention : `COUNT(*)` compte les lignes, `COUNT(colonne)` ignore les NULL.

**Sous-requêtes :**
```sql
-- dans le WHERE
SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE total > 100);

-- corrélée (exécutée par ligne, attention aux performances)
SELECT u.* FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- dans le FROM (table dérivée)
SELECT * FROM (SELECT city, COUNT(*) n FROM users GROUP BY city) t WHERE t.n > 5;
```
`EXISTS` est souvent plus performant que `IN` sur de gros volumes, car il s'arrête au premier résultat trouvé.

**CTE (Common Table Expression)** : bien plus lisible que des sous-requêtes imbriquées :
```sql
WITH ventes_par_client AS (
    SELECT client_id, SUM(montant) AS total
    FROM commandes GROUP BY client_id
)
SELECT c.nom, v.total
FROM clients c JOIN ventes_par_client v ON v.client_id = c.id
WHERE v.total > 1000;
```

**Fonctions de fenêtrage** : la partie qui distingue un candidat :
```sql
SELECT nom, service, salaire,
       RANK()       OVER (PARTITION BY service ORDER BY salaire DESC) AS rang,
       AVG(salaire) OVER (PARTITION BY service)                       AS moy_service,
       LAG(salaire) OVER (ORDER BY date_embauche)                     AS precedent
FROM employes;
```
Contrairement à `GROUP BY`, une fonction de fenêtrage **ne réduit pas le nombre de lignes** : elle ajoute une valeur calculée sur un ensemble lié à chaque ligne. Principales : `ROW_NUMBER`, `RANK`, `DENSE_RANK`, `LAG`/`LEAD`, `SUM/AVG OVER`.

Cas d'usage typique en entretien : *"le top 3 par catégorie"*, impossible proprement avec `GROUP BY`, immédiat avec `ROW_NUMBER() OVER (PARTITION BY categorie ORDER BY ...)` filtré dans une CTE.

**NULL** : `NULL` n'est égal à rien, pas même à lui-même. On teste avec `IS NULL` / `IS NOT NULL`. `COALESCE(a, b, 'défaut')` retourne la première valeur non nulle. Piège : `NOT IN` avec un NULL dans la liste ne retourne jamais rien.

**Modification :**
```sql
INSERT INTO t (a, b) VALUES (1, 2);
UPDATE t SET a = 1 WHERE id = 5;         -- ne JAMAIS oublier le WHERE
DELETE FROM t WHERE id = 5;
-- upsert PostgreSQL
INSERT INTO t (id, val) VALUES (1, 'x')
ON CONFLICT (id) DO UPDATE SET val = EXCLUDED.val;
```

**Transactions** : `BEGIN` → opérations → `COMMIT` ou `ROLLBACK`.

**Performance** : `EXPLAIN ANALYZE` pour le plan réel. Éviter `SELECT *` (charge des colonnes inutiles et empêche les index couvrants), éviter une fonction sur une colonne indexée dans le `WHERE` (`WHERE YEAR(date) = 2024` empêche l'usage de l'index, préférer `WHERE date >= '2024-01-01' AND date < '2025-01-01'`).

**Familles de commandes** : **DDL** (`CREATE`, `ALTER`, `DROP`, `TRUNCATE`), **DML** (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `MERGE`), **DCL** (`GRANT`, `REVOKE`), **TCL** (`COMMIT`, `ROLLBACK`, `SAVEPOINT`).

**DELETE vs TRUNCATE vs DROP** : `DELETE` supprime des lignes (avec `WHERE`, journalisé ligne par ligne, déclenche les triggers, annulable) ; `TRUNCATE` vide toute la table d'un coup (très rapide, réinitialise souvent les séquences, transactionnel en PostgreSQL mais pas en Oracle ni en MySQL) ; `DROP` supprime la table elle-même.

**Opérations ensemblistes** : `UNION` (fusionne et **déduplique**, donc tri ou hachage coûteux), `UNION ALL` (fusionne sans dédupliquer, à préférer quand c'est possible), `INTERSECT`, `EXCEPT` (`MINUS` en Oracle).

**CASE** : `SUM(CASE WHEN statut = 'PAYEE' THEN montant ELSE 0 END)` permet de faire des colonnes conditionnelles (pivot manuel). PostgreSQL propose aussi `COUNT(*) FILTER (WHERE ...)`.

**Requête récursive** (hiérarchie employé → manager, arborescence de catégories) :
```sql
WITH RECURSIVE hierarchie AS (
    SELECT id, nom, manager_id, 1 AS niveau FROM employes WHERE manager_id IS NULL
    UNION ALL
    SELECT e.id, e.nom, e.manager_id, h.niveau + 1
    FROM employes e JOIN hierarchie h ON e.manager_id = h.id
)
SELECT * FROM hierarchie;
```

**Auto-jointure** (employés et leur manager) :
```sql
SELECT e.nom AS employe, m.nom AS manager
FROM employes e LEFT JOIN employes m ON e.manager_id = m.id;
```

**Pagination par curseur (keyset)** :
```sql
SELECT * FROM commandes
WHERE (date_creation, id) < (:derniere_date, :dernier_id)
ORDER BY date_creation DESC, id DESC
LIMIT 20;
```
Avec un index sur `(date_creation, id)`, chaque page coûte le même prix, contrairement à `OFFSET 100000` qui lit et jette 100 000 lignes.

**Agrégats et NULL** : `SUM`, `AVG`, `MIN`, `MAX`, `COUNT(colonne)` ignorent les NULL ; `AVG` d'une colonne avec des NULL ne compte pas ces lignes au dénominateur. `SUM` sur zéro ligne renvoie NULL, d'où `COALESCE(SUM(x), 0)`. Agrégation de chaînes : `STRING_AGG(nom, ', ')` (PostgreSQL, SQL Server), `GROUP_CONCAT` (MySQL), `LISTAGG` (Oracle). Sous-totaux : `GROUP BY ROLLUP(a, b)`, `CUBE`, `GROUPING SETS`.

**Fonctions de fenêtrage, compléments** : différence entre `ROW_NUMBER` (1, 2, 3, 4 même en cas d'égalité), `RANK` (1, 2, 2, 4) et `DENSE_RANK` (1, 2, 2, 3). Cumul : `SUM(montant) OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)`. Moyenne glissante sur 7 lignes : `ROWS BETWEEN 6 PRECEDING AND CURRENT ROW`. `NTILE(4)` pour des quartiles, `FIRST_VALUE`/`LAST_VALUE`. On ne peut pas filtrer sur une fonction de fenêtrage dans le `WHERE` (elle est calculée après) : passer par une CTE ou une sous-requête.

**Upsert selon les bases** : `INSERT ... ON CONFLICT DO UPDATE` (PostgreSQL), `INSERT ... ON DUPLICATE KEY UPDATE` (MySQL), `MERGE` (standard SQL, Oracle, SQL Server, PostgreSQL 15+).

**Dates** : `CURRENT_DATE`, `NOW()`, `DATE_TRUNC('month', d)` (PostgreSQL), `EXTRACT(YEAR FROM d)`, intervalles `d + INTERVAL '7 days'`. Toujours filtrer par intervalle demi-ouvert (`>= debut AND < fin`) plutôt que `BETWEEN` sur des timestamps.

**Injection SQL** : l'exemple classique `"SELECT * FROM users WHERE nom = '" + nom + "'"` avec `nom = ' OR '1'='1`. Défense : requêtes paramétrées, partout, y compris en JPQL et en SQL natif ; un nom de colonne ou un sens de tri dynamique ne peut pas être paramétré, il se valide contre une liste blanche.

### En situation

- **"Écris une requête qui..."** → jointure, agrégation, fenêtrage.
- **"Cette requête est lente"** → EXPLAIN, index, SELECT *, fonction sur colonne.
- **"Trouve les doublons"** → GROUP BY + HAVING COUNT(*) > 1.
- **"Top N par catégorie"** → fonction de fenêtrage.

### Questions techniques

**Q : Ordre d'exécution logique d'une requête ?** R : FROM → JOIN → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT.

**Q : WHERE vs HAVING ?** R : WHERE filtre les lignes avant groupement ; HAVING filtre les groupes après agrégation.

**Q : INNER vs LEFT JOIN ?** R : INNER garde les correspondances des deux côtés ; LEFT garde tout à gauche avec NULL à droite.

**Q : Comment trouver les lignes sans correspondance ?** R : `LEFT JOIN ... WHERE droite.id IS NULL`.

**Q : COUNT(*) vs COUNT(colonne) ?** R : COUNT(*) compte les lignes ; COUNT(colonne) ignore les NULL.

**Q : IN vs EXISTS ?** R : EXISTS s'arrête au premier résultat, souvent plus performant sur de gros volumes.

**Q : Différence GROUP BY / fonction de fenêtrage ?** R : GROUP BY réduit le nombre de lignes ; la fenêtre ajoute une valeur calculée sans réduire.

**Q : Comment faire un top 3 par catégorie ?** R : `ROW_NUMBER() OVER (PARTITION BY cat ORDER BY x DESC)` dans une CTE, puis filtrer sur le rang.

**Q : Piège du NULL ?** R : NULL n'égale rien, même pas NULL. Tester avec IS NULL ; attention à NOT IN avec des NULL.

**Q : Pourquoi éviter SELECT * ?** R : Colonnes inutiles transférées, index couvrants inutilisables, et fragilité au changement de schéma.

**Q : DDL vs DML ?** R : Définition de la structure (CREATE, ALTER, DROP) vs manipulation des données (SELECT, INSERT, UPDATE, DELETE).

**Q : DELETE vs TRUNCATE vs DROP ?** R : Supprime des lignes (filtrable, triggers) / vide la table rapidement / supprime la table.

**Q : UNION vs UNION ALL ?** R : UNION déduplique (coûteux), UNION ALL garde tout.

**Q : ROW_NUMBER vs RANK vs DENSE_RANK ?** R : 1,2,3,4 / 1,2,2,4 / 1,2,2,3 en cas d'égalité.

**Q : Comment trouver le deuxième salaire le plus élevé ?** R : `SELECT DISTINCT salaire FROM employes ORDER BY salaire DESC LIMIT 1 OFFSET 1`, ou `DENSE_RANK() = 2` dans une CTE, ou `MAX(salaire) WHERE salaire < (SELECT MAX(salaire) ...)`.

**Q : Comment trouver le N-ième salaire par département ?** R : DENSE_RANK() OVER (PARTITION BY departement ORDER BY salaire DESC) dans une CTE, filtré sur le rang N.

**Q : Comment parcourir une hiérarchie en SQL ?** R : Avec une CTE récursive (WITH RECURSIVE).

**Q : Pourquoi OFFSET est-il lent sur de grandes valeurs ?** R : La base lit et ignore toutes les lignes précédentes ; la pagination keyset l'évite.

**Q : Que renvoie SUM sur aucune ligne ?** R : NULL ; utiliser COALESCE(SUM(x), 0).

**Q : Peut-on filtrer sur ROW_NUMBER() dans le WHERE ?** R : Non, la fenêtre est calculée après ; passer par une CTE ou une sous-requête.

**Q : Comment faire un cumul progressif ?** R : SUM(x) OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW).

**Q : Comment se protéger d'un tri dynamique injectable ?** R : Valider la colonne et le sens contre une liste blanche, car ils ne peuvent pas être paramétrés.

**Q : Comment faire un upsert en PostgreSQL ?** R : INSERT ... ON CONFLICT (cle) DO UPDATE SET ...

### Questions de situation

**Q : "Écris une requête donnant, pour chaque ville, le nombre de commandes des clients actifs, uniquement les villes avec plus de 10 commandes."**
R :
```sql
SELECT c.ville, COUNT(o.id) AS nb_commandes
FROM clients c
JOIN commandes o ON o.client_id = c.id
WHERE c.actif = true
GROUP BY c.ville
HAVING COUNT(o.id) > 10
ORDER BY nb_commandes DESC;
```
"Le point à expliquer : `WHERE` filtre les clients **avant** le groupement, `HAVING` filtre les villes **après** l'agrégation. Inverser les deux ne fonctionnerait pas, parce que l'agrégat n'existe pas encore au moment du `WHERE`."

**Q : "Comment tu trouves les doublons dans une table ?"**
R :
```sql
SELECT email, COUNT(*) 
FROM users 
GROUP BY email 
HAVING COUNT(*) > 1;
```
"Et pour récupérer les lignes complètes, ou n'en garder qu'une, j'utiliserais une fonction de fenêtrage :
```sql
WITH numerotes AS (
  SELECT id, email, ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) AS rn
  FROM users
)
DELETE FROM users WHERE id IN (SELECT id FROM numerotes WHERE rn > 1);
```
Ça conserve la première occurrence de chaque email et supprime les suivantes."

**Q : "Cette requête est lente, quelles pistes sans changer le schéma ?"**
R : "D'abord `EXPLAIN ANALYZE` pour voir ce que fait réellement le moteur.
Les problèmes fréquents et corrigeables côté requête : un `SELECT *` qui charge des colonnes inutiles et empêche d'utiliser un index couvrant ; une **fonction appliquée sur une colonne indexée** dans le `WHERE`, qui rend l'index inutilisable, `WHERE YEAR(date) = 2024` doit devenir un intervalle `date >= '2024-01-01' AND date < '2025-01-01'` ; une sous-requête corrélée exécutée pour chaque ligne, qu'on peut souvent réécrire en jointure ; ou un `NOT IN` sur une sous-requête contenant des NULL, qu'on remplace par `NOT EXISTS`.
Je vérifierais aussi la présence d'un `LIMIT` : ramener un million de lignes pour n'en afficher vingt est un gaspillage complet.
Et si le plan montre des estimations très éloignées du réel, un `ANALYZE` pour rafraîchir les statistiques peut suffire."

**Q : "Pour chaque client, donne sa dernière commande."**
R :
```sql
WITH classees AS (
    SELECT o.*, ROW_NUMBER() OVER (PARTITION BY client_id ORDER BY date_creation DESC) AS rn
    FROM commandes o
)
SELECT * FROM classees WHERE rn = 1;
```
"Version portable avec fenêtrage. En PostgreSQL, `SELECT DISTINCT ON (client_id) * FROM commandes ORDER BY client_id, date_creation DESC` fait la même chose. Un index sur `(client_id, date_creation DESC)` rend la requête efficace."

**Q : "Calcule le chiffre d'affaires mensuel et son évolution par rapport au mois précédent."**
R :
```sql
WITH mensuel AS (
    SELECT DATE_TRUNC('month', date_creation) AS mois, SUM(montant) AS ca
    FROM commandes
    GROUP BY 1
)
SELECT mois, ca,
       ca - LAG(ca) OVER (ORDER BY mois) AS evolution,
       ROUND(100.0 * (ca - LAG(ca) OVER (ORDER BY mois)) / NULLIF(LAG(ca) OVER (ORDER BY mois), 0), 1) AS evolution_pct
FROM mensuel
ORDER BY mois;
```
"`LAG` récupère la valeur de la ligne précédente, et `NULLIF(..., 0)` évite une division par zéro. Le premier mois a une évolution NULL, ce qui est correct."

**Q : "Trouve les clients qui n'ont jamais commandé."**
R : "Trois écritures : `LEFT JOIN commandes o ON o.client_id = c.id WHERE o.id IS NULL`, `WHERE NOT EXISTS (SELECT 1 FROM commandes o WHERE o.client_id = c.id)`, ou `NOT IN (SELECT client_id FROM commandes)`. J'éviterais la troisième : si une ligne de la sous-requête a un `client_id` NULL, `NOT IN` ne renvoie plus rien. `NOT EXISTS` est la plus sûre et en général aussi performante que la jointure."
