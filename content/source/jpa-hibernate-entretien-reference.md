---
subject: jpa-hibernate
title: JPA & Hibernate
category: frameworks
position: 60
status: published
---

# JPA & Hibernate : le document de référence pour les entretiens techniques

> Pour préparer un entretien : JPA, Hibernate et JDBC en profondeur.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. JPA, HIBERNATE & JDBC EN PROFONDEUR
═══════════════════════════════════════════

### Vue d'ensemble

JPA (Jakarta Persistence) est la spécification de l'ORM en Java, Hibernate son implémentation dominante. L'ORM promet de manipuler des objets plutôt que des lignes, et il tient cette promesse pour l'essentiel du code CRUD. Mais il le fait au prix d'une **abstraction qui fuit** : les relations, le chargement paresseux, le cache de premier niveau et le moment exact où les requêtes partent ne sont pas visibles dans le code, et c'est là que naissent les N+1, les `LazyInitializationException` et les mises à jour fantômes.

En entretien, on attend de savoir mapper correctement une relation, expliquer le cycle de vie d'une entité et le contexte de persistance, diagnostiquer un N+1, et savoir quand **ne pas** utiliser l'ORM (traitements de masse, rapports). JDBC reste la couche sous-jacente : savoir l'utiliser directement fait partie des bases.

### Cours

**JDBC, les fondamentaux** :
```java
String sql = "SELECT id, nom FROM client WHERE ville = ?";
try (Connection c = dataSource.getConnection();
     PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setString(1, ville);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            liste.add(new Client(rs.getLong("id"), rs.getString("nom")));
        }
    }
}
```
- **`PreparedStatement` vs `Statement`** : requête précompilée avec paramètres liés, donc **protection contre l'injection SQL** et réutilisation du plan. Ne jamais concaténer une valeur dans une requête.
- Transactions : `connection.setAutoCommit(false)`, puis `commit()` / `rollback()` ; par défaut chaque requête est committée seule.
- **Batch** : `ps.addBatch()` dans une boucle puis `executeBatch()`, des ordres de grandeur plus rapide que des inserts individuels.
- Toujours obtenir les connexions d'un **pool** (`DataSource`, HikariCP) et tout fermer en try-with-resources.
- `CallableStatement` pour les procédures stockées. `ResultSet` : curseur avant uniquement par défaut, `setFetchSize` pour lire de gros résultats par paquets.

**Mapping d'une entité** : `@Entity`, `@Table`, `@Id`, `@Column(nullable, length, unique)`, `@Enumerated(EnumType.STRING)`, `@Embedded`/`@Embeddable` (value object stocké dans les colonnes de l'entité, par exemple une adresse), `@Transient` (non persisté), `@Lob`, `@Version`. JPA exige un constructeur sans argument (au moins protected) et une classe non final (pour les proxys de chargement paresseux). Les **records** ne peuvent pas être des entités (immuables, final), mais servent très bien de projections et d'`@Embeddable` depuis Hibernate 6.2.

**Génération des identifiants** :
| Stratégie | Fonctionnement | Remarque |
|---|---|---|
| `IDENTITY` | colonne auto-incrémentée | l'ID n'est connu qu'après l'INSERT, ce qui **désactive le batch d'insertion** d'Hibernate |
| `SEQUENCE` | séquence en base | recommandé ; avec un `allocationSize` (50 par défaut), Hibernate réserve des plages d'IDs et peut batcher |
| `TABLE` | table de compteurs | lent, à éviter |
| `UUID` | `@GeneratedValue` avec `UUID` (JPA 3.1) ou `@UuidGenerator` | pas d'aller-retour en base ; préférer un UUID v7 ordonné |

**Relations** :
- `@ManyToOne` (le côté le plus fréquent, porte la clé étrangère), `@OneToMany`, `@OneToOne`, `@ManyToMany` (table de jointure ; dès qu'elle porte des attributs, la remplacer par une entité intermédiaire avec deux `@ManyToOne`).
- **Côté propriétaire** : celui qui détient la clé étrangère (le `@ManyToOne`) ; l'autre côté déclare `mappedBy` et n'est **pas** pris en compte pour l'écriture. Dans une relation bidirectionnelle, il faut **synchroniser les deux côtés** en mémoire, par des méthodes utilitaires (`commande.ajouterLigne(l)` qui fait `lignes.add(l)` et `l.setCommande(this)`).
- **Valeurs par défaut du fetch** : `@ManyToOne` et `@OneToOne` en **EAGER**, `@OneToMany` et `@ManyToMany` en **LAZY**. Bonne pratique : tout passer en `LAZY` (`@ManyToOne(fetch = FetchType.LAZY)`) et charger explicitement.
- Un `@OneToMany` **unidirectionnel** sans `@JoinColumn` crée une table de jointure inutile : préférer le bidirectionnel ou le `@ManyToOne` seul.
- **Cascade** : `PERSIST`, `MERGE`, `REMOVE`, `REFRESH`, `DETACH`, `ALL`. À poser du parent vers les enfants qu'il possède (une commande et ses lignes), jamais sur un `@ManyToOne` (supprimer une ligne ne doit pas supprimer la commande, ni le client).
- `orphanRemoval = true` : retirer un enfant de la collection le supprime en base.

**Cycle de vie et opérations de l'EntityManager** :
- `persist(e)` : une entité *transient* devient *managed* (INSERT au flush).
- `find(Classe, id)` : charge (ou renvoie depuis le contexte) ; `getReference` renvoie un proxy sans requête immédiate.
- `merge(e)` : copie l'état d'une entité *detached* dans une entité managée (qu'il charge si besoin) et **renvoie cette copie** ; l'objet passé reste détaché. Piège classique : continuer à modifier l'objet d'origine.
- `remove(e)` : *managed* → *removed* (DELETE au flush).
- `detach`, `clear`, `refresh`.
- **flush** : synchronisation du contexte vers la base (envoi des SQL), qui n'est pas un commit. Mode `AUTO` par défaut : flush avant le commit et avant une requête qui pourrait lire des données modifiées.

**Le contexte de persistance** = cache de premier niveau + *unit of work* + *identity map* : dans une transaction, une ligne correspond à un seul objet, les modifications sont détectées par **dirty checking** (comparaison avec un instantané pris au chargement) et regroupées au flush. Conséquence : charger 100 000 entités dans une transaction remplit la mémoire et rend chaque flush coûteux. Pour les traitements de masse : traiter par paquets avec `flush()` + `clear()` réguliers, activer `hibernate.jdbc.batch_size`, `order_inserts`/`order_updates`, ou utiliser une `StatelessSession`, JDBC, ou des requêtes bulk (`update ... where`).

**LazyInitializationException** : on accède à une relation paresseuse alors que la session est fermée (après la transaction, dans le contrôleur, pendant la sérialisation JSON). Solutions : charger ce qu'il faut dans le service (fetch join, entity graph), mapper vers un DTO **dans** la transaction. Mauvaises solutions : passer en EAGER (charge tout, partout), activer `open-in-view` (masque le problème), `hibernate.enable_lazy_load_no_trans` (une requête et une connexion par accès).

**N+1, toutes les solutions** : `JOIN FETCH` en JPQL (`select c from Commande c join fetch c.lignes where ...`), `@EntityGraph`, `@BatchSize(size = 50)` ou `hibernate.default_batch_fetch_size` (charge les collections par lots avec un `IN`), `@Fetch(FetchMode.SUBSELECT)`, ou une projection DTO. **Détecter** : activer les logs SQL en développement (`spring.jpa.show-sql`, ou mieux le logger `org.hibernate.SQL` et les statistiques Hibernate), compter les requêtes dans les tests (datasource-proxy, Hypersistence Utils).

**Pièges du fetch join** : deux collections en `JOIN FETCH` dans la même requête produisent un produit cartésien, et Hibernate lève `MultipleBagFetchException` si ce sont des `List` (*bags*) ; charger une collection par requête, ou utiliser des `Set`. Combiner `JOIN FETCH` sur une collection et pagination fait paginer **en mémoire** (avertissement *HHH90003004*) : paginer d'abord sur les identifiants, puis charger les collections pour ces identifiants.

**Requêtes** : **JPQL** (sur les entités et leurs attributs, pas sur les tables), **Criteria API** (construction programmatique typée, avec le métamodèle généré `Client_`), **SQL natif** (`createNativeQuery`), **requêtes nommées** (`@NamedQuery`, vérifiées au démarrage). Projections DTO : `select new com.x.ClientDto(c.id, c.nom) from Client c`, ou des records avec Hibernate 6.

**Héritage** :
| Stratégie | Schéma | Avantages / inconvénients |
|---|---|---|
| `SINGLE_TABLE` (défaut) | une table, colonne discriminante | rapide, pas de jointure ; colonnes nullables |
| `JOINED` | une table par classe, jointures | normalisé ; jointures à chaque lecture |
| `TABLE_PER_CLASS` | une table par classe concrète | requêtes polymorphes en UNION, lentes |
| `@MappedSuperclass` | pas d'entité parente | partage de champs communs (id, audit) sans polymorphisme en requête |

**equals et hashCode d'une entité** : ne pas les baser sur un identifiant généré (null avant persist, donc le hashCode change après l'insertion et l'entité se perd dans un HashSet). Options : une **clé métier** naturelle et immuable (un numéro de commande), ou un identifiant attribué à la création (UUID), ou equals sur l'id avec un hashCode **constant** par classe. Et jamais de `@Data` de Lombok sur une entité.

**Cache de second niveau** : partagé entre sessions (au niveau de l'`EntityManagerFactory`), via un fournisseur (Ehcache, Infinispan, Caffeine via JCache), activé par entité avec `@Cacheable` / `@Cache(usage = READ_WRITE)`. Utile pour des données de référence très lues et rarement modifiées ; dangereux si d'autres applications écrivent dans la base (le cache ne le sait pas) ou en multi-instance sans cache distribué. Le **cache de requêtes** stocke les identifiants résultats d'une requête et s'invalide à chaque modification des tables concernées.

**Verrouillage** : optimiste avec `@Version` (`OptimisticLockException`), pessimiste avec `LockModeType.PESSIMISTIC_WRITE` (traduit en `SELECT ... FOR UPDATE`), avec un timeout de verrou.

**Callbacks et écouteurs** : `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostLoad`, `@PreRemove`, `@EntityListeners` (audit). Ils ne sont pas déclenchés par les requêtes bulk ni le SQL natif.

**Hibernate 6 et 7** : Hibernate 6 a réécrit le moteur de requêtes (SQM, meilleur support du SQL), changé certaines stratégies de génération d'identifiants et le mapping de types ; Hibernate 7 implémente Jakarta Persistence 3.2 (base de Spring Boot 4). **`ddl-auto`** (`create`, `create-drop`, `update`, `validate`, `none`) : jamais `update` en production, `validate` vérifie que le schéma (géré par Flyway/Liquibase) correspond aux entités.

**Quand ne pas utiliser l'ORM** : rapports et agrégations complexes, imports/exports massifs, requêtes très spécifiques au moteur. Alternatives : JDBC/`JdbcClient`, **jOOQ** (SQL typé généré depuis le schéma), **MyBatis** (SQL écrit à la main, mapping automatique).

### En situation

- **"Une page déclenche 300 requêtes"** → N+1 → fetch join, entity graph, batch size, DTO.
- **"LazyInitializationException dans le contrôleur"** → charger dans le service, mapper en DTO dans la transaction.
- **"L'insertion de 100 000 lignes prend 10 minutes"** → IDENTITY empêche le batch, contexte qui grossit → SEQUENCE, batch_size, flush/clear, ou JDBC.
- **"Une modification n'est pas sauvegardée"** → entité détachée et merge dont on ignore le retour, ou modification hors transaction.
- **"Une modification est sauvegardée alors qu'on n'a pas appelé save"** → dirty checking sur une entité managée.
- **"Les entités disparaissent d'un HashSet après persist"** → hashCode basé sur un id généré.

### Questions techniques

**Q : JPA vs Hibernate ?** R : La spécification vs son implémentation principale.

**Q : PreparedStatement vs Statement ?** R : Paramètres liés et requête précompilée (protection contre l'injection, réutilisation du plan) vs SQL concaténé.

**Q : Comment faire un insert massif en JDBC ?** R : addBatch puis executeBatch, dans une transaction.

**Q : Pourquoi IDENTITY empêche-t-il le batch d'insertion ?** R : Hibernate doit exécuter chaque INSERT immédiatement pour connaître l'identifiant généré.

**Q : Quelle stratégie d'ID recommander ?** R : SEQUENCE avec un allocationSize adapté, ou un UUID v7 attribué par l'application.

**Q : Pourquoi une entité ne peut-elle pas être final ?** R : Hibernate crée des sous-classes proxy pour le chargement paresseux.

**Q : C'est quoi le côté propriétaire d'une relation ?** R : Celui qui porte la clé étrangère ; c'est lui qui est pris en compte à l'écriture, l'autre déclare mappedBy.

**Q : Fetch par défaut des relations ?** R : ManyToOne et OneToOne en EAGER, OneToMany et ManyToMany en LAZY.

**Q : Pourquoi mettre les ManyToOne en LAZY ?** R : Pour éviter de charger des graphes entiers et des N+1 cachés.

**Q : Où placer une cascade REMOVE ?** R : Du parent vers les enfants qu'il possède, jamais vers une entité partagée (ManyToOne).

**Q : cascade REMOVE vs orphanRemoval ?** R : Supprimer les enfants avec le parent vs supprimer un enfant retiré de la collection.

**Q : persist vs merge ?** R : Rend managée une entité nouvelle vs copie l'état d'une entité détachée dans une entité managée et renvoie celle-ci.

**Q : find vs getReference ?** R : Charge l'entité (ou null) vs renvoie un proxy sans requête immédiate (exception à l'accès si inexistante).

**Q : flush vs commit ?** R : Envoi des SQL à la base vs validation définitive de la transaction.

**Q : C'est quoi le contexte de persistance ?** R : Le cache de premier niveau et l'unit of work d'une session : une instance par ligne, dirty checking, écriture au flush.

**Q : Comment traiter un million d'entités sans saturer la mémoire ?** R : Par paquets avec flush/clear, batch JDBC, StatelessSession, ou requêtes bulk.

**Q : Causes et solutions d'une LazyInitializationException ?** R : Accès à une relation lazy session fermée ; charger explicitement (fetch join, entity graph) ou mapper en DTO dans la transaction.

**Q : Toutes les solutions au N+1 ?** R : JOIN FETCH, @EntityGraph, @BatchSize / default_batch_fetch_size, SUBSELECT, projections DTO.

**Q : C'est quoi une MultipleBagFetchException ?** R : L'erreur levée quand on fetch-joint plusieurs collections de type List dans une même requête.

**Q : Que se passe-t-il avec JOIN FETCH + pagination sur une collection ?** R : Hibernate pagine en mémoire après avoir tout chargé ; paginer d'abord les identifiants.

**Q : JPQL vs SQL ?** R : JPQL interroge les entités et attributs, portable ; SQL interroge les tables.

**Q : Les stratégies d'héritage JPA ?** R : SINGLE_TABLE, JOINED, TABLE_PER_CLASS, et @MappedSuperclass sans polymorphisme.

**Q : Pourquoi ne pas baser hashCode sur l'id généré ?** R : Il est null avant persist, donc le hashCode change et l'entité devient introuvable dans un Set.

**Q : Cache de premier vs second niveau ?** R : Par session/transaction, toujours actif vs partagé entre sessions, optionnel et configuré par entité.

**Q : Risque du cache de second niveau ?** R : Données périmées si la base est modifiée par d'autres applications ou par du SQL natif.

**Q : Les callbacks sont-ils déclenchés par une requête bulk ?** R : Non, ni les callbacks ni les cascades.

**Q : Valeurs de ddl-auto et laquelle en production ?** R : create, create-drop, update, validate, none ; validate ou none, avec Flyway/Liquibase.

**Q : Les records peuvent-ils être des entités ?** R : Non (immuables et final), mais ils servent de projections et d'embeddables.

**Q : Quand préférer jOOQ ou MyBatis ?** R : Pour un SQL complexe et maîtrisé (rapports, requêtes spécifiques) où l'ORM gêne plus qu'il n'aide.

### Questions de situation

**Q : "Mappe une commande avec ses lignes de commande et son client."**
R :
```java
@Entity
class Commande {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) Long id;
    @Version Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Client client;                                   // pas de cascade vers le client

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    List<LigneCommande> lignes = new ArrayList<>();

    void ajouterLigne(LigneCommande l) { lignes.add(l); l.commande = this; }
    void retirerLigne(LigneCommande l) { lignes.remove(l); l.commande = null; }
}

@Entity
class LigneCommande {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Commande commande;                               // côté propriétaire
    String produit; int quantite;
}
```
"La commande est l'agrégat : elle possède ses lignes, d'où la cascade et `orphanRemoval`. Le client est une entité indépendante, donc ni cascade ni EAGER. Les méthodes d'ajout synchronisent les deux côtés. `@Version` protège des modifications concurrentes, et la séquence permet le batch d'insertion des lignes."

**Q : "Un traitement nocturne met à jour le statut de 2 millions de commandes et dure des heures."**
R : "Si la règle s'exprime en SQL, une **requête bulk** unique (`update Commande c set c.statut = :s where c.date < :d`) fait le travail en secondes, sans charger aucune entité. Attention : elle contourne les callbacks, le `@Version` et le cache, ce qu'il faut accepter ou compenser.
Si chaque commande demande une logique Java, je traiterais **par paquets** : lecture paginée par curseur sur l'identifiant, traitement, `flush()` et `clear()` tous les 500 à 1 000 éléments pour que le contexte de persistance ne grossisse pas, `hibernate.jdbc.batch_size` activé pour regrouper les UPDATE, une transaction par paquet pour pouvoir reprendre. Spring Batch apporte ce cadre avec la reprise sur erreur."

**Q : "Les logs montrent un UPDATE sur des entités que personne n'a modifiées."**
R : "C'est le dirty checking qui détecte une différence. Causes fréquentes : un setter ou une méthode `@PostLoad` qui normalise une valeur au chargement, un type mutable mal comparé (un `Date`, un tableau, un champ JSON converti), un `AttributeConverter` dont la conversion aller-retour ne redonne pas exactement la même valeur, ou une collection remplacée par une nouvelle instance.
Je chercherais quelles colonnes changent dans l'UPDATE, et je corrigerais la source. Pour une lecture pure, `@Transactional(readOnly = true)` désactive le dirty checking et supprime le problème au passage."
