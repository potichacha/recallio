---
subject: observabilite
title: Observabilité
category: cloud-devops
position: 130
status: draft
---

# Observabilité : le document de référence pour les entretiens techniques

> Pour préparer un entretien : logs, métriques, traces et alertes.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. OBSERVABILITÉ
═══════════════════════════════════════════

### Vue d'ensemble

Le **monitoring** répond à des questions qu'on a prévues à l'avance ("le CPU dépasse-t-il 80% ?"). L'**observabilité** vise plus haut : pouvoir répondre à des questions qu'on n'avait **pas** anticipées, à partir de ce que le système émet. C'est devenu indispensable avec les microservices : dans un monolithe, une pile d'appels suffit à comprendre ; dans un système distribué, une requête traverse dix services et aucun ne détient l'histoire complète.

Les trois piliers se complètent plutôt qu'ils ne se substituent : les **métriques** disent qu'il y a un problème et à quelle ampleur, les **traces** disent où, les **logs** disent quoi exactement. Un bon dispositif permet de passer de l'un à l'autre, d'une alerte à la trace concernée, puis aux logs de la requête.

### Cours

**Logs** : événements textuels horodatés.
- Niveaux : TRACE < DEBUG < INFO < WARN < ERROR.
- **Structured logging** (JSON) : chaque log porte des champs (timestamp, niveau, service, traceId, utilisateur), ce qui rend l'indexation et la recherche efficaces.
- **MDC** (Mapped Diagnostic Context) : attacher automatiquement un identifiant de corrélation à tous les logs d'une requête.
- Stack : collecte (Fluentd, Filebeat) → stockage (Elasticsearch, Loki) → visualisation (Kibana, Grafana).
- Bonnes pratiques : ne jamais logger de données sensibles, éviter le logging excessif dans les chemins chauds, toujours logger le stacktrace avec l'erreur.

**Métriques** : valeurs numériques agrégées dans le temps. Prometheus collecte par **scraping** d'un endpoint exposé par l'application.
- **Counter** : ne fait qu'augmenter (nombre de requêtes, d'erreurs).
- **Gauge** : monte et descend (connexions actives, taille de file).
- **Histogram** : distribution, permettant de calculer des percentiles (p50, p95, p99).
- **Summary** : percentiles calculés côté application.

Le réflexe important : raisonner en **percentiles**, pas en moyenne. Une moyenne de 100 ms peut cacher que 5% des utilisateurs attendent 3 secondes.

**Traces** : le parcours d'une requête à travers les services. Une **trace** regroupe des **spans**, chacun représentant une opération (appel HTTP, requête SQL). La **propagation de contexte** transmet l'identifiant de trace entre services via des en-têtes HTTP standardisés (`traceparent`).

**OpenTelemetry** est le standard unifié d'instrumentation (SDK + protocole), indépendant du backend (Jaeger, Tempo, Zipkin). C'est ce qui évite de s'enfermer chez un fournisseur.

**Les 4 signaux dorés** (Google SRE) : latence, trafic, erreurs, saturation. C'est la base d'un tableau de bord utile.

**SLI / SLO / SLA** : l'indicateur mesuré, l'objectif interne visé, l'engagement contractuel envers le client. Un **error budget** dérive du SLO : si l'objectif est 99,9%, on tolère 0,1% d'indisponibilité, et ce budget arbitre entre livrer vite et stabiliser.

**Alerting** : alerter sur des **symptômes visibles par l'utilisateur** (taux d'erreur, latence) plutôt que sur des causes possibles (CPU élevé, qui peut être parfaitement normal). Trop d'alertes non actionnables produisent de la fatigue d'alerte, et les vraies passent inaperçues.

**Logging en Java** : **SLF4J** est une façade (l'API qu'on utilise dans le code : `LoggerFactory.getLogger(...)`), avec une implémentation au choix : **Logback** (défaut de Spring Boot), **Log4j 2** (performant, appenders asynchrones), ou `java.util.logging`. Utiliser les **placeholders** `log.debug("Commande {} créée pour {}", id, client)` plutôt que la concaténation, pour ne pas construire la chaîne si le niveau est désactivé ; passer l'exception en dernier argument pour avoir la stack trace. Les ponts (`jul-to-slf4j`, `log4j-over-slf4j`) unifient les logs des bibliothèques.

**Log4Shell** (CVE-2021-44228, décembre 2021) : Log4j 2 interprétait des expressions `${jndi:ldap://...}` contenues dans les messages loggés, permettant l'exécution de code à distance simplement en faisant logger une chaîne (un en-tête User-Agent par exemple). Leçons : ne jamais interpréter des données utilisateur, connaître ses dépendances transitives (SBOM), pouvoir patcher vite.

**Quoi logger, à quel niveau** : ERROR pour ce qui demande une intervention, WARN pour une anomalie gérée, INFO pour les événements métier significatifs et le cycle de vie, DEBUG pour le diagnostic. Jamais de mot de passe, jeton, numéro de carte ou donnée personnelle (RGPD) ; masquer si nécessaire. Logger une exception **une seule fois**, à l'endroit où elle est traitée (sinon la même erreur apparaît cinq fois).

**Méthodes d'analyse de métriques** : **RED** pour les services (*Rate*, *Errors*, *Duration*) et **USE** pour les ressources (*Utilization*, *Saturation*, *Errors*), complémentaires des quatre signaux dorés.

**Micrometer** : la façade de métriques de l'écosystème Java (le "SLF4J des métriques"), utilisée par Spring Boot et Quarkus, avec des exporteurs vers Prometheus, Datadog, OTLP... Types : `Counter`, `Gauge`, `Timer` (durée + nombre, avec histogrammes de percentiles), `DistributionSummary`. **Observation API** (Micrometer 1.10+) : une seule instrumentation qui produit à la fois métriques et traces.

**Cardinalité** : chaque combinaison de labels d'une métrique crée une série temporelle. Mettre un identifiant utilisateur ou une URL complète en label (au lieu du gabarit `/users/{id}`) crée des millions de séries et fait exploser Prometheus. Les labels doivent avoir peu de valeurs possibles.

**PromQL de base** : `rate(http_server_requests_seconds_count[5m])` (débit), `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))` (p95), `sum by (status) (...)`.

**OpenTelemetry, compléments** : API + SDK + protocole **OTLP** + **Collector** (reçoit, transforme, échantillonne et exporte vers un ou plusieurs backends). L'**agent Java** instrumente automatiquement HTTP, JDBC, Kafka, etc., sans modifier le code. **Échantillonnage** : *head-based* (décidé au début de la trace, simple) vs *tail-based* (décidé à la fin dans le collector, permet de garder toutes les traces en erreur ou lentes). **Exemplars** : relier un point de métrique à une trace précise. Propagation via l'en-tête W3C `traceparent` (et `baggage` pour des données de contexte).

**Corrélation** : injecter le `traceId` dans le MDC pour qu'il apparaisse dans chaque ligne de log, ce qui permet de passer d'une trace aux logs correspondants.

**Stacks courantes** : Prometheus + Grafana + Alertmanager ; Loki (logs) ; Tempo ou Jaeger (traces) ; ELK/OpenSearch ; solutions commerciales (Datadog, Dynatrace, New Relic, Elastic APM). **Profilage continu** (Pyroscope, async-profiler en continu, JFR) comme quatrième signal.

**Alerting, compléments** : alerter sur le **taux de consommation de l'error budget** (*burn rate*) plutôt que sur un seuil instantané : une alerte rapide si le budget mensuel se consume en quelques heures, une alerte lente si la dérive est durable. Chaque alerte doit être actionnable et liée à un *runbook*.

### En situation

- **"Comment tu sais que ton service va bien ?"** → signaux dorés, SLO, alertes.
- **"Débugger un problème qui n'arrive qu'en prod"** → logs, traces, corrélation.
- **"Où est le goulot dans une chaîne de services ?"** → distributed tracing.
- **"Que logger et à quel niveau ?"** → stratégie de logging.

### Questions techniques

**Q : Monitoring vs observabilité ?** R : Répondre à des questions prévues vs pouvoir répondre à des questions non anticipées.

**Q : Les 3 piliers ?** R : Logs (quoi), métriques (combien), traces (où).

**Q : Types de métriques Prometheus ?** R : Counter, Gauge, Histogram, Summary.

**Q : Pourquoi les percentiles plutôt que la moyenne ?** R : La moyenne masque les cas lents qui dégradent l'expérience d'une minorité d'utilisateurs.

**Q : Trace vs span ?** R : Parcours complet d'une requête vs opération individuelle dans ce parcours.

**Q : C'est quoi la propagation de contexte ?** R : La transmission du traceId entre services via les en-têtes HTTP.

**Q : Les 4 signaux dorés ?** R : Latence, trafic, erreurs, saturation.

**Q : SLI, SLO, SLA ?** R : Indicateur mesuré, objectif interne, engagement contractuel.

**Q : Sur quoi alerter ?** R : Sur des symptômes visibles par l'utilisateur, pas sur des causes potentielles.

**Q : SLF4J vs Logback ?** R : La façade (API) vs une implémentation.

**Q : Pourquoi utiliser des placeholders dans les logs ?** R : Pour ne pas construire la chaîne si le niveau est désactivé.

**Q : C'était quoi Log4Shell ?** R : Une faille de Log4j 2 qui exécutait du code via des expressions JNDI contenues dans les messages loggés.

**Q : Pourquoi logger une exception une seule fois ?** R : Pour éviter des doublons trompeurs ; on la logge là où elle est traitée.

**Q : RED vs USE ?** R : Débit, erreurs, durée pour les services vs utilisation, saturation, erreurs pour les ressources.

**Q : C'est quoi Micrometer ?** R : La façade de métriques (et d'observations) de l'écosystème Java, multi-backends.

**Q : Pourquoi ne pas mettre un identifiant utilisateur en label ?** R : Explosion de la cardinalité : une série temporelle par valeur.

**Q : Comment calculer un p95 en PromQL ?** R : histogram_quantile(0.95, ...) sur les buckets d'un histogramme.

**Q : Rôle du collector OpenTelemetry ?** R : Recevoir, transformer, échantillonner et exporter la télémétrie vers les backends.

**Q : Head-based vs tail-based sampling ?** R : Décision au début de la trace vs à la fin, ce qui permet de garder les traces en erreur ou lentes.

**Q : Comment relier logs et traces ?** R : En injectant le traceId dans le MDC de chaque log.

**Q : C'est quoi un burn rate ?** R : La vitesse de consommation de l'error budget, base d'alertes plus pertinentes que des seuils.

### Questions de situation

**Q : "Débugger un problème qui n'arrive qu'en production."**
R : "Cas le plus délicat, puisqu'on ne peut pas reproduire à volonté.
Je commencerais par **rassembler les preuves** : logs, métriques et traces au moment de l'incident. Avec du tracing distribué, je peux suivre une requête problématique de bout en bout et identifier le service ou l'appel responsable, c'est souvent ce qui fait gagner le plus de temps.
Puis **caractériser** : tous les utilisateurs ou un sous-ensemble ? Quelle fréquence ? Corrélation avec un déploiement, un pic de charge, une heure précise ? Beaucoup de bugs 'impossibles' sont des problèmes de concurrence qui n'apparaissent que sous charge réelle.
Ensuite **identifier les écarts prod/dev** : volume de données, configuration, latence réseau, ressources, versions. Le bug est presque toujours dans un de ces écarts.
Si je ne trouve pas, j'**instrumente** : logs ciblés ou métrique sur le chemin suspect, puis redéploiement pour collecter. Mieux vaut un déploiement d'observabilité qu'une correction devinée.
Exemple classique : un tracing OpenTelemetry qui fonctionne en local mais pas une fois déployé sur Kubernetes, à cause de la propagation de contexte entre services ou de la configuration du collector."

**Q : "Que surveilles-tu pour savoir qu'un service va bien ?"**
R : "Je partirais des **signaux visibles par l'utilisateur** plutôt que de métriques techniques isolées.
Les quatre essentiels : **taux d'erreur**, **latence** en p95 et p99 (pas la moyenne), **débit**, et **saturation** (CPU, mémoire, pool de connexions, taille des files).
Au-delà, quelques métriques **métier** valent souvent mieux que dix métriques d'infrastructure : par exemple le nombre de commandes validées par minute. Une chute là-dessus révèle un problème qu'aucune métrique système ne montrerait.
Pour les **alertes**, je les baserais sur des symptômes : 'le taux d'erreur dépasse X%' plutôt que 'le CPU dépasse 80%', qui peut être parfaitement normal. Et je définirais un **SLO** avec un error budget, ce qui donne un critère objectif pour arbitrer entre livrer et stabiliser.
Enfin, je m'assurerais que chaque log porte un **identifiant de corrélation**, pour relier un ticket utilisateur à la trace exacte de sa requête."

**Q : "La facture de l'outil d'observabilité a triplé en un mois."**
R : "Je chercherais trois causes. Une **explosion de cardinalité** des métriques (un label à valeurs illimitées ajouté récemment), visible en listant les métriques au plus grand nombre de séries. Un **volume de logs** devenu excessif (un DEBUG oublié en production, une boucle qui logge, des stack traces répétées). Une absence d'**échantillonnage** des traces.
Corrections : supprimer ou regrouper les labels fautifs, revoir les niveaux de log et filtrer au collecteur, échantillonner les traces en tail-based en gardant les erreurs. Et mettre en place un suivi du volume par service pour détecter la prochaine dérive avant la facture."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **APM** | Application Performance Monitoring : outils de supervision applicative |
| **OTLP / OTel** | OpenTelemetry Protocol / OpenTelemetry |
| **RED / USE** | Rate-Errors-Duration / Utilization-Saturation-Errors |
| **SLI / SLO / SLA** | Indicateur / objectif interne / engagement contractuel |
