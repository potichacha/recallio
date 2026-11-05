---
subject: kafka
title: Messaging & Kafka
category: data
position: 80
status: draft
---

# Messaging & Kafka : le document de référence pour les entretiens techniques

> Pour préparer un entretien : messaging, files de messages et Kafka.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. MESSAGING & KAFKA
═══════════════════════════════════════════

### Vue d'ensemble

La communication synchrone crée un **couplage temporel** : si le service appelé est indisponible, l'appelant l'est aussi. Le messaging brise ce couplage, l'émetteur dépose un message et poursuit son travail ; le consommateur le traite quand il peut. On gagne en résilience, en absorption des pics, et en capacité à ajouter des consommateurs sans toucher au producteur.

Le prix à payer est réel : le flux global devient plus difficile à suivre, la cohérence devient différée, et il faut gérer des cas nouveaux, un message livré deux fois, un message dans le désordre, un message qu'aucun consommateur n'arrive à traiter.

Deux familles d'outils. Les **brokers de messages** classiques (RabbitMQ) distribuent des messages qui disparaissent une fois consommés. **Kafka** fonctionne différemment : c'est un **journal ordonné et persistant** que les consommateurs lisent à leur rythme, chacun gardant sa position. Cette différence explique la plupart de ses propriétés, plusieurs consommateurs indépendants, possibilité de rejouer l'historique, très haut débit.

### Cours

**Concepts Kafka :**
- **Topic** : un flux nommé de messages.
- **Partition** : un topic est découpé en partitions, unité de parallélisme et d'ordre. **L'ordre n'est garanti qu'à l'intérieur d'une partition**, pas globalement.
- **Clé de partitionnement** : les messages de même clé vont dans la même partition, c'est ainsi qu'on garantit l'ordre par entité (tous les événements d'une commande donnée restent ordonnés entre eux).
- **Offset** : la position d'un message dans une partition. Le consommateur mémorise son offset, ce qui permet de reprendre où il en était.
- **Consumer group** : un groupe se répartit les partitions ; chaque partition n'est lue que par un consommateur du groupe. Ajouter des consommateurs au-delà du nombre de partitions ne sert à rien.
- **Réplication** : chaque partition est répliquée sur plusieurs brokers pour la tolérance aux pannes.
- **Rétention** : les messages sont conservés selon une durée ou une taille, indépendamment de leur consommation, d'où la possibilité de rejouer.

**Garanties de livraison :**
- **At most once** : au plus une fois, on peut perdre.
- **At least once** : au moins une fois, on peut recevoir des doublons, le mode courant.
- **Exactly once** : possible dans Kafka sous conditions (transactions), mais coûteux et limité.

En pratique on travaille en *at least once*, et on rend les **consommateurs idempotents** : traiter deux fois le même message doit produire le même résultat. C'est l'exigence la plus importante du messaging.

**Dead Letter Queue** : les messages qu'on n'arrive pas à traiter après plusieurs tentatives sont mis de côté plutôt que de bloquer indéfiniment la partition, et analysés séparément.

**Consumer lag** : l'écart entre le dernier message produit et celui traité. C'est **la métrique clé** : un lag qui croît signifie que la consommation ne suit pas le rythme de production.

**Pattern Outbox** : pour publier de façon fiable, on écrit l'événement dans une table de la base **dans la même transaction** que la donnée métier, puis un processus séparé le publie. Sans ça, on peut committer en base et perdre le message (ou l'inverse).

**RabbitMQ** en comparaison : routage plus riche (échanges, files, clés de routage), messages supprimés après consommation, adapté à de la distribution de tâches ; Kafka est adapté aux flux d'événements à fort volume avec rejeu.

**Architecture, KRaft** : historiquement, Kafka dépendait de **ZooKeeper** pour ses métadonnées et l'élection du contrôleur. Le mode **KRaft** (consensus Raft intégré) le remplace, et **Kafka 4.0 (2025) a supprimé ZooKeeper** : un cluster ne compte plus que des brokers et des contrôleurs Kafka.

**Réplication, détail** : chaque partition a un **leader** (qui reçoit écritures et lectures par défaut) et des **followers** ; l'**ISR** (*in-sync replicas*) est l'ensemble des réplicas à jour. Si le leader tombe, un réplica de l'ISR est élu.

**Producteur, les réglages qui comptent** :
- `acks=0` (n'attend rien, pertes possibles), `acks=1` (le leader a écrit, perte possible si le leader meurt avant réplication), `acks=all` (tous les réplicas ISR ont écrit, avec `min.insync.replicas=2` côté topic pour une vraie durabilité). Défaut `all` depuis Kafka 3.0.
- **Producteur idempotent** (`enable.idempotence=true`, défaut depuis 3.0) : évite les doublons et les inversions d'ordre causés par les retries du producteur.
- Débit : `batch.size`, `linger.ms` (attendre un peu pour remplir les lots), `compression.type` (lz4, zstd).
- **Transactions** (`transactional.id`) : écrire atomiquement dans plusieurs partitions et committer les offsets dans la même transaction, base de l'*exactly once* dans un pipeline lecture-traitement-écriture Kafka. Les consommateurs lisent alors en `isolation.level=read_committed`. L'exactly once ne s'étend pas aux effets externes (une écriture en base, un email) : là, il faut l'idempotence.
- Partitionnement : par hachage de la clé ; sans clé, répartition par lots entre partitions (*sticky partitioner*).

**Consommateur, les réglages qui comptent** :
- **Commit des offsets** : automatique (`enable.auto.commit=true`, périodique, risque de perdre ou de rejouer des messages en cas de crash) ou manuel **après traitement** (`commitSync`/`commitAsync`, ou `AckMode` en Spring Kafka), qui donne du *at least once*.
- `auto.offset.reset` : `earliest` ou `latest`, pour un groupe qui n'a pas encore d'offset.
- `max.poll.records`, `max.poll.interval.ms` : si le traitement d'un lot dépasse cet intervalle, le consommateur est considéré comme mort et exclu du groupe, d'où un **rebalancing** et le retraitement des messages. Cause fréquente de doublons en boucle.
- **Rebalancing** : redistribution des partitions quand un consommateur rejoint ou quitte le groupe ; le protocole **coopératif** (incrémental) évite d'arrêter tout le groupe ; `group.instance.id` (membre statique) évite un rebalancing lors d'un simple redémarrage.

**Rétention vs compaction** : la rétention supprime par **durée** ou **taille** ; la **compaction** (`cleanup.policy=compact`) garde au moins le **dernier message de chaque clé**, ce qui fait du topic une table d'état reconstituable (un message à valeur nulle, *tombstone*, supprime la clé).

**Ordre et retries côté consommateur** : réessayer un message en échec en bloquant la partition préserve l'ordre mais arrête tout ; envoyer vers un **topic de retry** (avec délai) puis une DLQ ne bloque pas, mais casse l'ordre pour la clé concernée. Choix à faire selon le besoin métier (Spring Kafka fournit `@RetryableTopic` et `DefaultErrorHandler`).

**Schémas** : **Schema Registry** (Confluent, Apicurio) avec Avro, Protobuf ou JSON Schema ; règles de compatibilité (*backward* : un nouveau consommateur lit les anciens messages ; *forward* ; *full*), qui empêchent un producteur de publier un message que les consommateurs ne sauraient pas lire.

**Écosystème** : **Kafka Connect** (connecteurs source et puits sans code : base de données, S3, Elasticsearch), **Debezium** (CDC des bases vers Kafka), **Kafka Streams** (bibliothèque Java de traitement de flux : `KStream`, `KTable`, jointures, fenêtres, état local), Flink (traitement de flux distribué), ksqlDB. Côté Java : client `kafka-clients`, **Spring Kafka** (`KafkaTemplate`, `@KafkaListener`, gestion des erreurs, transactions), SmallRye Reactive Messaging (Quarkus). **Files de type "queue"** : Kafka 4.0 a introduit en accès anticipé les *share groups* (KIP-932), où plusieurs consommateurs peuvent traiter les messages d'une même partition avec acquittement individuel.

**RabbitMQ, détail** : le producteur publie vers un **exchange**, qui route vers des **queues** selon des **bindings** : `direct` (clé de routage exacte), `topic` (motifs avec jokers `*` et `#`), `fanout` (diffusion à toutes les queues liées), `headers`. Acquittements manuels (`ack`/`nack`), *prefetch* pour la répartition équitable, files durables et messages persistants, *dead letter exchange*, TTL, files à priorité, *quorum queues* (répliquées, recommandées), *streams* (journal rejouable proche de Kafka). Protocole AMQP 0-9-1.

**Autres** : JMS (API standard Java, ActiveMQ Artemis), Apache Pulsar (séparation calcul/stockage, multi-tenant), NATS, files managées du cloud (SQS, Pub/Sub, Service Bus).

### En situation

- **"Sync ou async ?"** → selon le besoin de réponse immédiate.
- **"Comment garantir qu'un message n'est pas perdu ?"** → outbox, accusés, réplication.
- **"Le consommateur prend du retard"** → lag, partitions, parallélisme.
- **"Un message est traité deux fois"** → idempotence.

### Questions techniques

**Q : Kafka vs RabbitMQ ?** R : Kafka est un journal persistant relu à la demande ; RabbitMQ distribue des messages consommés puis supprimés.

**Q : Où l'ordre est-il garanti dans Kafka ?** R : À l'intérieur d'une partition seulement.

**Q : À quoi sert la clé de partitionnement ?** R : À envoyer les messages de même clé dans la même partition, donc à garantir l'ordre par entité.

**Q : C'est quoi un consumer group ?** R : Un ensemble de consommateurs se répartissant les partitions d'un topic.

**Q : Combien de consommateurs utiles par topic ?** R : Au maximum autant que de partitions ; au-delà, ils restent inactifs.

**Q : Les garanties de livraison ?** R : At most once, at least once, exactly once. En pratique at least once + idempotence.

**Q : C'est quoi le consumer lag ?** R : L'écart entre le dernier message produit et le dernier traité, la métrique de santé principale.

**Q : À quoi sert une DLQ ?** R : Isoler les messages non traitables pour ne pas bloquer le flux.

**Q : Le pattern Outbox ?** R : Écrire l'événement en base dans la même transaction que la donnée, puis le publier, pour ne jamais perdre un message.

**Q : Kafka a-t-il encore besoin de ZooKeeper ?** R : Non, Kafka 4.0 l'a supprimé au profit du mode KRaft.

**Q : C'est quoi l'ISR ?** R : L'ensemble des réplicas d'une partition à jour avec le leader.

**Q : acks=0, 1, all ?** R : Aucune attente / écriture sur le leader / écriture sur tous les réplicas ISR.

**Q : Rôle de min.insync.replicas ?** R : Nombre minimal de réplicas à jour pour accepter une écriture en acks=all.

**Q : Que garantit le producteur idempotent ?** R : Pas de doublon ni d'inversion d'ordre dus aux retries du producteur (par partition).

**Q : Kafka garantit-il l'exactly once vers une base de données ?** R : Non, seulement au sein de Kafka (transactions) ; vers l'extérieur il faut un consommateur idempotent.

**Q : Auto-commit vs commit manuel ?** R : Périodique, avec perte ou rejeu possible vs après traitement, pour du at least once maîtrisé.

**Q : Pourquoi un consommateur retraite-t-il les mêmes messages en boucle ?** R : Souvent un traitement plus long que max.poll.interval.ms : exclusion, rebalancing et rejeu.

**Q : C'est quoi un rebalancing ?** R : La redistribution des partitions entre les consommateurs d'un groupe.

**Q : Rétention vs compaction ?** R : Suppression par âge ou taille vs conservation du dernier message de chaque clé.

**Q : C'est quoi un tombstone ?** R : Un message à valeur nulle qui supprime une clé d'un topic compacté.

**Q : À quoi sert un Schema Registry ?** R : Stocker les schémas des messages et vérifier la compatibilité de leurs évolutions.

**Q : Kafka Connect vs Kafka Streams ?** R : Connecteurs d'intégration sans code vs bibliothèque Java de traitement de flux.

**Q : Les types d'exchange RabbitMQ ?** R : direct, topic, fanout, headers.

**Q : Retry bloquant vs topic de retry ?** R : Préserve l'ordre mais bloque la partition vs ne bloque pas mais casse l'ordre pour la clé.

### Questions de situation

**Q : "Comment tu choisis entre synchrone et asynchrone entre deux services ?"**
R : "Je pars du besoin métier : l'appelant a-t-il **besoin de la réponse pour continuer** ?
Si oui (vérifier un solde avant de valider une commande) c'est synchrone, en REST ou gRPC. Simple à comprendre et à débugger, mais couplage temporel : si l'appelé tombe, l'appelant est affecté. D'où timeouts et circuit breaker obligatoires.
Si non (envoyer une confirmation, indexer, générer un rapport) c'est asynchrone. Le consommateur peut être indisponible sans impacter l'appelant, et on absorbe mieux les pics.
Le coût de l'asynchrone : flux global plus difficile à suivre, ordre à gérer, idempotence des consommateurs obligatoire, et on ne sait pas immédiatement si le traitement a réussi.
Donc je ne le choisis pas par défaut, seulement quand le découplage apporte une valeur réelle."

**Q : "Un consommateur accumule du retard sur un topic."**
R : "Je regarderais d'abord le **consumer lag** pour mesurer l'ampleur et la tendance, stable, croissant, en pic ?
Ensuite la cause. Si le traitement par message est trop lent, j'optimiserais ce traitement, souvent un appel base ou réseau non batché. Si c'est un problème de débit, j'augmenterais le **parallélisme** : mais le plafond est le nombre de **partitions** : ajouter des consommateurs au-delà ne sert à rien, il faut alors repartitionner le topic.
Je vérifierais aussi qu'un message empoisonné ne bloque pas la partition en échouant en boucle, c'est exactement le rôle d'une **DLQ**.
Et je m'assurerais que le lag est **surveillé avec alerte**, parce qu'un retard qui grossit finit par dépasser la rétention, et là on perd des messages définitivement."

**Q : "Comment tu garantis qu'un événement de commande est traité une et une seule fois par le service de facturation ?"**
R : "Exactement une fois de bout en bout n'existe pas gratuitement ; je viserais **au moins une fois + idempotence**, ce qui produit l'effet d'exactement une fois.
Côté production : outbox pour ne jamais perdre l'événement, producteur idempotent et `acks=all`. Côté consommation : commit des offsets **après** traitement. Et l'idempotence : chaque événement porte un identifiant unique ; dans la **même transaction** que la création de la facture, j'enregistre cet identifiant dans une table des événements traités avec une contrainte unique. Un événement rejoué viole la contrainte et est ignoré. La clé de partitionnement est l'identifiant de commande, pour que tous les événements d'une commande restent ordonnés."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **AMQP** | Advanced Message Queuing Protocol : protocole de messagerie de RabbitMQ |
| **CDC** | Change Data Capture : capture des modifications d'une base depuis son journal (Debezium) |
| **DLQ** | Dead Letter Queue : file des messages non traitables |
| **ISR** | In-Sync Replicas : réplicas Kafka à jour |
| **KRaft** | Mode de consensus de Kafka sans ZooKeeper |
