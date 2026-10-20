# CLAUDE.md : application de flashcards pour réviser les entretiens techniques

> Ce fichier est le cahier des charges et le contrat de collaboration du projet. Lis-le en entier au début de chaque session.
> Le contenu source des questions est dans `content/source/java-entretien-reference.md`.

---

## 1. Comment nous travaillons ensemble (à respecter en priorité)

Je suis développeur backend Java. **C'est moi qui écris le code.** Ton rôle est celui d'un développeur senior qui m'accompagne : tu m'aides à concevoir, tu m'expliques, tu me guides et tu relis.

**Ce que tu fais :**
- Proposer un plan découpé en **petites étapes** livrables (une étape = une pull request de taille raisonnable), et attendre ma validation avant de passer à la suite.
- Pour chaque étape : expliquer **le but**, **les fichiers et classes à créer ou modifier**, **les choix de conception et leurs alternatives**, **les pièges à éviter**, et **comment vérifier que c'est terminé** (tests, commandes à lancer).
- Me renvoyer vers la documentation officielle quand c'est pertinent (Quarkus, PostgreSQL, React, FSRS...).
- Relire mon code quand je te le demande : bugs, lisibilité, maintenabilité, sécurité, tests manquants, écarts avec les conventions de ce fichier. Commente comme en revue de code (bloquant vs suggestion).
- Me poser des questions quand une exigence est floue, plutôt que de supposer.
- Signaler quand une de mes décisions s'écarte de ce document ou crée de la dette, en proposant de consigner la décision dans un ADR.

**Ce que tu ne fais pas, sauf si je le demande explicitement :**
- Écrire du code de production complet à ma place. Tu peux montrer une **signature**, un **squelette**, du **pseudo-code** ou un **extrait court** (une dizaine de lignes) pour illustrer une notion.
- Modifier des fichiers sans me l'avoir proposé.
- Ajouter une dépendance, une technologie ou une fonctionnalité hors du périmètre de la phase en cours.
- Avancer de plusieurs étapes d'un coup.

Si je te dis "écris-le", "montre-moi la solution" ou "génère ce fichier", tu peux le faire, mais seulement pour ce qui est demandé. Pour du code répétitif sans valeur pédagogique (configuration, boilerplate de build, fichiers YAML), tu peux me proposer de le générer.

**Objectif personnel du projet** : progresser techniquement et avoir un projet de portfolio exemplaire. Privilégie toujours la solution **la plus claire et la plus maintenable**, pas la plus astucieuse.

---

## 2. Le produit

### 2.1 Vision
Une application web (PWA, utilisable sur mobile) pour **réviser les entretiens techniques** de façon quotidienne et ludique :
- des cartes déjà rédigées, orientées questions d'entretien (Java pour commencer, d'autres sujets plus tard : C++, IA, prompting...) ;
- plusieurs formats de questions (flashcard, QCM, "qu'affiche ce code ?", question de situation ouverte) ;
- une **répétition espacée** pour mémoriser durablement ;
- de la **gamification** (points, streak quotidien) ;
- plus tard, des **groupes** avec classement et un **mode live** chronométré entre membres ;
- à terme, une offre pour les entreprises (ESN qui préparent leurs consultants aux entretiens clients, équipes qui entretiennent leur niveau).

### 2.2 Utilisateurs
- **Candidat / développeur** : révise seul, chaque jour.
- **Créateur de groupe** : crée un groupe, invite des membres, choisit les thèmes, la difficulté et les règles.
- **Membre de groupe** : révise et participe aux défis du groupe.
- **Admin d'organisation** (phase entreprise) : gère les utilisateurs de son entreprise, consulte des statistiques agrégées, ajoute des questions propres à l'entreprise.

### 2.3 Hors périmètre de la V1
Mode live, exécution de code, correction des réponses ouvertes par IA, SSO entreprise, multi-organisation, paiement, application mobile native. Ne pas anticiper ces fonctionnalités dans le code au-delà de ce qui est décrit en section 4 (pas de sur-ingénierie, principe YAGNI).

### 2.4 Philosophie anti-triche
Il est impossible d'empêcher totalement l'usage d'une IA ou d'internet dans un navigateur. Règles produit :
- en **révision personnelle**, aucune mesure anti-triche : tricher ne nuit qu'au tricheur ;
- dans les **groupes**, les classements valorisent la **régularité** (streak, révisions faites) plus que le score brut ;
- le **mode live** (phase 3), synchronisé et chronométré, est le seul contexte compétitif ;
- pas de surveillance intrusive (webcam, détection invasive).

---

## 3. Stack technique (décidée)

| Domaine | Choix | Remarque |
|---|---|---|
| Langage backend | **Java 25** (LTS) | records pour les DTO et value objects, pas de Lombok |
| Framework backend | **Quarkus** (dernière version stable 3.x) | Quarkus REST, Hibernate ORM (sans Panache active record, voir 4.3), Hibernate Validator, SmallRye Health, OpenAPI, Micrometer |
| Build | **Maven** avec le wrapper `mvnw`, multi-modules si nécessaire | versions centralisées dans le pom parent |
| Base de données | **PostgreSQL** | migrations **Flyway**, jamais `generation` automatique du schéma hors tests |
| Authentification | **OIDC standard**, backend agnostique du fournisseur | Keycloak via Dev Services en local ; fournisseur managé en production (à choisir, voir section 10) ; le backend valide des JWT avec `quarkus-oidc` |
| Frontend | **React + TypeScript + Vite**, PWA | TanStack Query (appels API), React Router, Tailwind CSS, client API **généré** depuis l'OpenAPI |
| Tests backend | JUnit 6, AssertJ, Mockito, `@QuarkusTest`, REST Assured, **Testcontainers** (via Dev Services), **ArchUnit** | |
| Tests frontend | Vitest, Testing Library ; Playwright pour quelques parcours de bout en bout | |
| Qualité | Spotless (formatage), Error Prone + NullAway avec JSpecify (optionnel, à décider en phase 0), ESLint + Prettier côté front | |
| CI | **GitHub Actions** | build, tests, analyse, image Docker |
| Conteneurs | Docker, `docker compose` pour le local | une image backend, une image frontend (fichiers statiques servis par un serveur web) |
| Hébergement | PaaS ou VPS européen (Clever Cloud, Scaleway ou OVHcloud), PostgreSQL managé | décision à prendre en phase 1, consignée en ADR |
| Observabilité | logs JSON sur stdout, métriques Micrometer, Sentry pour les erreurs (plus tard OpenTelemetry) | |
| Phase 2+ | **Valkey/Redis** (classements, temps réel), WebSockets Next (mode live) | ne pas l'introduire avant la phase concernée |

Toute nouvelle dépendance doit être justifiée et, si elle est structurante, faire l'objet d'un ADR.

---

## 4. Architecture et principes de maintenabilité

La maintenabilité est l'exigence numéro un. Le code doit rester compréhensible par quelqu'un qui le découvre dans six mois.

### 4.1 Monolithe modulaire
Une seule application backend, découpée en **modules métier** aux frontières explicites. Pas de microservices.

Modules prévus (créés au fur et à mesure des phases, pas tous dès le départ) :
| Module | Responsabilité | Phase |
|---|---|---|
| `content` | thèmes, cartes, versions de cartes, import du contenu | 1 |
| `review` | sessions de révision, planification par répétition espacée (FSRS), historique | 1 |
| `user` | profil utilisateur lié à l'identité OIDC, préférences (fuseau horaire) | 1 |
| `gamification` | points, streaks, succès | 1 |
| `group` | groupes, membres, invitations, réglages, classements | 2 |
| `live` | parties synchronisées en temps réel | 3 |
| `organization` | multi-entreprise, administration, statistiques agrégées | 4 |

### 4.2 Organisation du code : par fonctionnalité, puis par couche
```
com.<projet>.<module>
├── domain/          # modèle métier pur : entités, value objects, règles, ports (interfaces)
├── application/     # cas d'usage (un service par cas d'usage ou par groupe cohérent), transactions
├── infrastructure/  # adapters : persistance JPA, clients externes, implémentation des ports
└── api/             # contrôleurs REST, DTO d'entrée/sortie, mapping, gestion d'erreurs
```
Règles (vérifiées par des tests **ArchUnit**) :
- `domain` ne dépend d'**aucun** framework (ni Quarkus, ni JPA, ni Jackson) : c'est du Java pur, testable unitairement.
- Les dépendances vont vers l'intérieur : `api` → `application` → `domain` ← `infrastructure`.
- Un module n'accède à un autre module **que par son API publique** (une façade ou des interfaces du package racine du module), jamais à ses classes internes ni à ses tables.
- Pas de cycle entre modules.
- Pas d'entité JPA exposée dans l'API REST : toujours des DTO.

Si une règle devient gênante, on en discute et on consigne l'exception dans un ADR, on ne la contourne pas en silence.

### 4.3 Conventions de code
- Java 25 moderne : `record`, `sealed` + `switch` exhaustif pour les types de cartes et les résultats, `var` quand le type est évident, `Optional` uniquement en type de retour.
- Injection par **constructeur**, champs `final`.
- Entités JPA séparées du modèle de domaine **si** le domaine est riche (module `review`) ; acceptable de mapper directement pour les modules simples. Décision par module, documentée.
- Pas de Panache active record (logique de persistance dans les entités) ; si Panache est utilisé, en mode **repository**.
- Le temps passe toujours par un `java.time.Clock` injecté (indispensable pour tester les streaks et la planification).
- Les dates sont stockées en UTC (`timestamptz`) ; le fuseau horaire de l'utilisateur sert à calculer "le jour" d'un streak.
- Erreurs : exceptions métier explicites, traduites par un `ExceptionMapper` global au format **RFC 9457 (Problem Details)** avec un code d'erreur stable.
- Validation des entrées avec Bean Validation sur les DTO.
- Logs : SLF4J/JBoss Logging, messages paramétrés, aucune donnée personnelle ni jeton dans les logs.
- Nommage en anglais dans le code (classes, méthodes, tables) ; documentation et ADR en français.
- Pas de nombre magique, pas de méthode de plus d'une quarantaine de lignes sans raison, pas de classe "fourre-tout".

### 4.4 API
- REST, ressources au pluriel, codes HTTP corrects, pagination bornée côté serveur.
- **Contract-first** : la spécification OpenAPI vit dans le dépôt (`backend/src/main/openapi/` ou équivalent) et sert à générer le client TypeScript du frontend. Toute évolution de l'API passe par une modification du contrat. (Alternative à discuter en phase 0 : code-first avec génération de la spec ; dans les deux cas, le client front est généré, jamais écrit à la main.)
- Versioning par URI (`/api/v1/...`) dès le départ.
- Les endpoints qui modifient la progression (enregistrer une réponse) sont **idempotents** (identifiant de requête côté client) pour supporter les retries sur mobile.

### 4.5 Sécurité
- Toutes les routes sauf la santé et le contenu public nécessitent un JWT valide.
- Autorisation vérifiée **côté serveur pour chaque ressource** (un utilisateur ne lit que sa progression, un membre ne voit que ses groupes).
- Secrets uniquement en variables d'environnement, jamais dans le dépôt.
- Dépendances surveillées (Renovate ou Dependabot), scan des images en CI.
- En-têtes de sécurité et CORS configurés explicitement.

### 4.6 Documentation vivante
- `README.md` : comment lancer le projet en local en moins de cinq minutes.
- `docs/adr/` : un fichier par décision structurante (`0001-monolithe-modulaire.md`, `0002-fsrs.md`...), format : contexte, options, décision, conséquences.
- `docs/architecture.md` : schéma des modules (C4 niveau conteneurs et composants), mis à jour à chaque phase.
- Les tests servent de spécification : noms explicites (`shouldResetStreakWhenADayIsMissed`).

### 4.7 Workflow Git
- `main` toujours déployable, protégée ; une branche courte par étape ; pull request même en solo (relecture par Claude).
- **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- La CI doit être verte avant le merge.

---

## 5. Contenu : de la source Markdown aux cartes

### 5.1 Format de la source
Le fichier `content/source/java-entretien-reference.md` est structuré ainsi :
- Chapitres : un titre `# N. TITRE` encadré de lignes de séparation `═══...`.
- Dans chaque chapitre, des sections `### Vue d'ensemble`, `### Cours`, `### En situation`, `### Questions techniques`, `### Questions de situation`.
- **Questions techniques** : une ligne `**Q : question ?** R : réponse.` Parfois la question contient un bloc de code juste après `**Q :**` et la réponse suit sur la ligne `R : ...` (chapitre 14, questions pièges).
- **Questions de situation** : `**Q : "énoncé"**` puis `R : "réponse rédigée"` sur plusieurs lignes, parfois avec des blocs de code, jusqu'à la question suivante ou la fin du chapitre.
- Le glossaire (tableau Markdown) peut devenir un deck de cartes "définition".

### 5.2 Pipeline d'import
- Un **outil d'import** (module `content`, exécutable en ligne de commande ou en tâche d'admin) parse la source et produit des cartes.
- Le parseur est du code **pur et entièrement testé unitairement** (fixtures Markdown dans les tests), car c'est la partie la plus fragile.
- Chaque carte reçoit un **identifiant stable** (dérivé du chapitre et d'un hash normalisé de la question), pour que la progression des utilisateurs survive aux réimports et aux corrections de texte. Une modification de texte crée une **nouvelle version** de la carte, pas une nouvelle carte.
- L'import est **idempotent** : relancer l'import ne crée aucun doublon.
- Un rapport d'import liste les cartes ajoutées, modifiées, retirées et les lignes non reconnues.
- À terme, le contenu pourra être migré vers un format plus structuré (un fichier YAML ou Markdown par carte dans `content/`), relu par pull request. Ne pas le faire en V1, mais garder le parseur isolé derrière une interface pour pouvoir changer de source.

### 5.3 Types de cartes
| Type | Origine | Évaluation |
|---|---|---|
| `FLASHCARD` | questions techniques | auto-évaluation (voir 6.1) |
| `CODE_OUTPUT` | questions pièges avec code (chapitre 14) | auto-évaluation en V1 ; QCM généré ou saisie libre plus tard |
| `MULTIPLE_CHOICE` | à créer (manuellement ou dérivé) | correction automatique |
| `OPEN_SCENARIO` | questions de situation | rédaction libre puis comparaison avec la réponse modèle et auto-évaluation |
| `DEFINITION` | glossaire | auto-évaluation |

Modéliser le type avec une interface `sealed` côté domaine.

---

## 6. Règles métier

### 6.1 Révision
- Une **session** propose les cartes **dues** (selon la planification) puis de nouvelles cartes, dans la limite d'un nombre quotidien de nouvelles cartes réglable.
- Pour une carte en auto-évaluation : l'utilisateur voit la question, réfléchit (ou écrit sa réponse), révèle la réponse, puis note : **À revoir / Difficile / Bien / Facile** (les quatre notes de FSRS).
- Filtrage par thème (chapitre) et par type de carte.

### 6.2 Répétition espacée : FSRS
- Utiliser l'algorithme **FSRS** (celui des versions récentes d'Anki). État par couple (utilisateur, carte) : stabilité, difficulté, date d'échéance, nombre de révisions, nombre d'oublis, état (nouvelle, en apprentissage, en révision, réapprentissage).
- L'algorithme est implémenté (ou intégré via une bibliothèque, à évaluer en phase 1 et à consigner dans un ADR) dans le **domaine**, en Java pur, avec des tests qui vérifient les intervalles produits sur des scénarios connus.
- Chaque réponse est enregistrée dans un **journal de révision** append-only (permettra de réentraîner les paramètres FSRS et de produire des statistiques).

### 6.3 Gamification
- **Points** : attribués à chaque révision (barème simple, configurable, défini en phase 1). Stockés comme des **événements** dans un registre append-only (`points_ledger`) ; le total est une somme ou une projection, jamais un compteur modifié en place.
- **Streak** : un jour compte si l'utilisateur a terminé au moins un objectif minimal (par exemple 10 cartes) **dans son fuseau horaire**. Un jour manqué remet le streak à zéro. Plus tard : "gel de streak" à gagner.
- La gamification ne doit jamais pousser à bâcler : pas de points pour aller plus vite en révision personnelle.

### 6.4 Groupes (phase 2)
- Un créateur crée un groupe, génère un lien d'invitation (expirant), définit : thèmes inclus, types de cartes, objectif quotidien, visibilité du classement (nominatif, anonymisé, désactivé).
- Classement hebdomadaire basé sur la régularité et les points.
- Défis de groupe : une série de cartes commune, avec un délai de réponse par carte.

### 6.5 Mode live (phase 3)
- Partie synchronisée façon quiz : un hôte lance, les membres rejoignent, chaque question a un chrono court, points selon la justesse et la rapidité, classement en direct.
- Uniquement des cartes à correction automatique (`MULTIPLE_CHOICE`, `CODE_OUTPUT` en QCM).

---

## 7. Modèle de données (première ébauche, à affiner)

Tables principales de la V1 (noms en anglais, `snake_case`, identifiants UUID v7 ou séquences, à décider en ADR) :
- `theme` (id, slug, title, position)
- `card` (id stable, theme_id, type, created_at, retired_at)
- `card_version` (id, card_id, version, question, answer, payload JSON pour les choix ou le code, created_at)
- `app_user` (id, oidc_subject unique, display_name, timezone, created_at)
- `review_state` (user_id, card_id, stability, difficulty, due_at, reps, lapses, state, last_review_at) : clé primaire (user_id, card_id)
- `review_log` (id, user_id, card_id, card_version_id, rating, reviewed_at, elapsed_ms, request_id unique)
- `points_ledger` (id, user_id, amount, reason, source_id, occurred_at)
- `daily_activity` (user_id, local_date, cards_reviewed, goal_reached) : base du calcul du streak

Phases suivantes : `study_group`, `group_member`, `group_settings`, `group_invitation`, `challenge`, `live_session`, `organization`.

Toute évolution du schéma passe par une migration Flyway versionnée, rétrocompatible avec la version du code précédente (ajouter avant de supprimer).

---

## 8. Stratégie de tests

| Niveau | Ce qu'on teste | Outils |
|---|---|---|
| Unitaire (le plus nombreux) | domaine : FSRS, calcul des streaks, points, parseur Markdown | JUnit, AssertJ |
| Intégration | repositories, migrations, endpoints REST, sécurité (accès refusé aux ressources d'autrui) | `@QuarkusTest`, REST Assured, PostgreSQL réel via Dev Services/Testcontainers |
| Architecture | règles de dépendances entre couches et modules | ArchUnit |
| Frontend | composants et hooks | Vitest, Testing Library |
| Bout en bout | 2 ou 3 parcours critiques (se connecter, réviser une session, voir son streak) | Playwright |

Règles : chaque bug corrigé reçoit un test qui le reproduit ; pas de `Thread.sleep` dans les tests ; le temps est piloté par un `Clock` fixe ; la couverture est un indicateur, pas un objectif.

---

## 9. Feuille de route

Chaque phase se termine par une démo fonctionnelle, une CI verte, la documentation à jour et une rétrospective courte (ce qui a marché, ce qu'on change).

### Phase 0 : fondations
- Dépôt monorepo : `backend/`, `frontend/`, `content/`, `docs/`, `infra/` (compose local).
- Projet Quarkus, structure de modules, premier test ArchUnit, Spotless, profil de dev avec PostgreSQL et Keycloak en Dev Services.
- Projet React + TypeScript + Vite, lint, tests, PWA minimale.
- CI GitHub Actions (build + tests backend et frontend).
- ADR 0001 (monolithe modulaire) et ADR 0002 (stack).
- **Terminé quand** : `./mvnw quarkus:dev` et `npm run dev` lancent tout en local, un endpoint `/api/v1/health` est appelé par le front, la CI est verte.

### Phase 1 : MVP révision solo
1. Parseur et import du contenu (avec rapport), consultation des thèmes et cartes.
2. Authentification OIDC de bout en bout, création du profil au premier login.
3. Session de révision avec auto-évaluation et planification FSRS.
4. Points, objectif quotidien et streak.
5. Tableau de bord personnel (cartes dues, streak, progression par thème).
6. Déploiement sur l'hébergeur choisi, observabilité de base.
- **Terminé quand** : quelques testeurs peuvent réviser chaque jour sur mobile ; on mesure combien reviennent après 7 jours.

### Phase 2 : groupes
Groupes, invitations, réglages, classements (Valkey/Redis introduit ici si nécessaire), défis de groupe, cartes QCM.

### Phase 3 : mode live
WebSockets, parties synchronisées, chrono, classement en direct, tests de charge simples.

### Phase 4 : offre entreprise
Multi-organisation (colonne `organization_id` + Row-Level Security PostgreSQL), SSO entreprise via le fournisseur OIDC, tableau de bord admin avec statistiques **agrégées**, questions privées par organisation, export, conformité RGPD (registre, suppression de compte, DPA).

### Phase 5 : options
Exécution de code dans un bac à sable isolé (Judge0 ou Piston), correction des réponses ouvertes par IA, nouveaux sujets (C++, IA, prompting), paiement.

---

## 10. Décisions encore ouvertes

À trancher au moment indiqué, avec un ADR :
1. Contract-first OpenAPI ou génération de la spec depuis le code (phase 0).
2. NullAway/Error Prone activés dès le départ ou plus tard (phase 0).
3. Type des identifiants : UUID v7 ou séquences (phase 1, avant la première migration métier).
4. FSRS implémenté soi-même ou bibliothèque existante (phase 1).
5. Fournisseur OIDC de production : managé (Zitadel Cloud, Clerk, Auth0...) ou Keycloak hébergé (phase 1, avant le déploiement).
6. Hébergeur (phase 1).
7. Nom du projet.

---

## 11. Première session

Pour notre première session en mode plan :
1. Lis ce fichier et parcours rapidement la structure du fichier de contenu (titres de chapitres, format des questions).
2. Pose-moi les questions nécessaires sur les points flous.
3. Propose le plan détaillé de la **phase 0**, découpé en étapes, avec pour chaque étape : objectif, fichiers concernés, notions à comprendre, critères de fin.
4. Attends ma validation avant de détailler la première étape.
