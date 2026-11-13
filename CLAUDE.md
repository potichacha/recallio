# CLAUDE.md : application de flashcards pour réviser les entretiens techniques

> Ce fichier est le cahier des charges et le contrat de collaboration du projet. Lis-le en entier au début de chaque session.
> Le contenu source des questions est dans `content/source/` : un document de référence par **sujet** (Java, Go...), voir la section 5.

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
- des cartes déjà rédigées, orientées questions d'entretien, réparties en **sujets** : langages, frameworks, cloud, fondamentaux, IA... (Java et Go pour commencer, d'autres ajoutés petit à petit, voir 2.5) ;
- plusieurs formats de questions (flashcard, QCM, "qu'affiche ce code ?", question de situation ouverte) ;
- une **répétition espacée** pour mémoriser durablement ;
- un **Tour** par langage pour l'apprendre pas à pas, comme le Go Tour : des pages courtes, un programme à modifier et à exécuter sur chacune, et quelques questions pour valider chaque notion, comme sur Ornikar (ADR 0037) ; un Tour « Programmer de zéro » pour qui n'a jamais programmé ;
- de la **gamification** (XP par sujet, streak quotidien) ;
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
- le **mode live** (phase 3), synchronisé et chronométré, est un contexte compétitif ; le **mode classé** (phase 3 ter, ADR 0025) en sera le second. Les deux n'utilisent que des formats corrigés par le serveur, avec un chrono mesuré par le serveur ;
- pas de surveillance intrusive (webcam, détection invasive).

### 2.5 Un catalogue multi-sujets
Recallio n'est pas une application « Java » : c'est un **catalogue de sujets d'entretien** qui s'enrichit au fil du temps. Chaque sujet est un document de référence rédigé dans le même format (section 5), importé indépendamment des autres.

- **Hiérarchie** : **catégorie** → **sujet** → **thème** (un chapitre du document) → **carte**.
  - Exemple : *Langages* → *Go* → *7. Concurrence : goroutines, channels & select* → « Que se passe-t-il si on écrit dans un channel fermé ? ».
- **Catégories** (liste fermée, extensible par ADR) :

| Catégorie | Exemples de sujets |
|---|---|
| Langages | Java, Go, C, C#, C++, Python, Kotlin, TypeScript, Rust |
| Frameworks | Spring Boot, Quarkus, Angular, React |
| Cloud & DevOps | Docker, Kubernetes, CI/CD |
| Données | SQL & PostgreSQL, NoSQL, Kafka |
| Cybersécurité | sécurité des applications (ADR 0052) |
| Fondamentaux | Programmer de zéro, algorithmes & structures de données, architecture & design |
| IA | Machine learning, LLM & prompting |

- **Ce que voit l'utilisateur** :
  - il **suit un sujet en l'ouvrant** : au premier login, guidé par son but (débuter, apprendre un autre langage, préparer un entretien), ou depuis les pages Cours, Tour et Révisions ; son profil les liste (ADR 0052) ;
  - ses révisions puisent dans ces sujets uniquement ;
  - il peut aussi réviser un sujet ou un thème en particulier.
- **Progression** : par sujet et par thème (tableau de bord), et streak global (un seul objectif quotidien, tous sujets confondus).
- **Publication** : un sujet peut exister en **brouillon** (importé, visible seulement par les admins) avant d'être **publié**. On peut ainsi relire un nouveau document en conditions réelles.
- **Annonce** : les pages Cours, Tour et Révisions montrent aussi les brouillons à tous, grisés, « work in progress » ; leur contenu reste réservé aux admins. Un sujet annoncé peut n'avoir encore que son `subject.yaml` (ADR 0052).
- **Ajouter un sujet ne demande aucun code** : un nouveau fichier dans `content/source/`, décrit par son en-tête (5.4), relu par pull request. Si un sujet a besoin d'un comportement particulier, c'est un signe que le format doit évoluer (ADR), pas qu'il faut un cas spécial dans le parseur.
- **Groupes et mode live** (phases 2 et 3) : un groupe choisit ses sujets (puis ses thèmes) ; une partie live porte sur un sujet.

---

## 3. Stack technique (décidée)

| Domaine | Choix | Remarque |
|---|---|---|
| Langage backend | **Java 25** (LTS, voir ADR 0007) | records pour les DTO et value objects, pas de Lombok |
| Framework backend | **Quarkus** (dernière version stable 3.x) | Quarkus REST, Hibernate ORM (sans Panache active record, voir 4.3), Hibernate Validator, SmallRye Health, OpenAPI, Micrometer |
| Build | **Maven** avec le wrapper `mvnw`, multi-modules si nécessaire | versions centralisées dans le pom parent |
| Base de données | **PostgreSQL** | migrations **Flyway**, jamais `generation` automatique du schéma hors tests |
| Authentification | **OIDC standard**, backend agnostique du fournisseur | Keycloak via Dev Services en local ; fournisseur managé en production (à choisir, voir section 10) ; le backend valide des JWT avec `quarkus-oidc` |
| Frontend | **React + TypeScript + Vite**, PWA | TanStack Query (appels API), React Router, Tailwind CSS, client API **généré** depuis l'OpenAPI, CodeMirror 6 (éditeur des exercices, chargé à la demande) ; design system « Rico » (ADR 0058, qui révise l'ADR 0049) : tokens Tailwind des thèmes clair et sombre, au choix dans le profil, Nunito et JetBrains Mono servies par l'application, icônes Phosphor, la mascotte Rico |
| Tests backend | JUnit 6, AssertJ, Mockito, `@QuarkusTest`, REST Assured, **Testcontainers** (via Dev Services), **ArchUnit** | |
| Tests frontend | Vitest, Testing Library ; Playwright pour quelques parcours de bout en bout, axe-core pour l'accessibilité (ADR 0050) | |
| Qualité | Spotless (formatage), Error Prone + NullAway avec JSpecify (dès la phase 0, voir ADR 0004), ESLint + Prettier côté front | |
| CI | **GitHub Actions** | build, tests, analyse, image Docker |
| Conteneurs | Docker, `docker compose` pour le local | une image backend, une image frontend (fichiers statiques servis par un serveur web) |
| Hébergement | PaaS ou VPS européen (Clever Cloud, Scaleway ou OVHcloud), PostgreSQL managé | décision à prendre en phase 1, consignée en ADR |
| Observabilité | logs JSON sur stdout, métriques Micrometer, Sentry pour les erreurs (plus tard OpenTelemetry) | |
| Exécution du code | service **`runner`** (Quarkus) et images **`sandbox/java`**, **`sandbox/go`**, **`sandbox/cpp`** : un conteneur jetable par exécution, pour juger un exercice par ses tests (ADR 0030, 0033, 0053) ou lancer un programme du Tour (ADR 0037) | seul composant à accéder à Docker ; joint par le seul backend |
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
| `content` | catégories, sujets, thèmes, cartes, versions de cartes, import du contenu | 1 |
| `review` | sessions de révision, planification par répétition espacée (FSRS), historique | 1 |
| `user` | profil utilisateur lié à l'identité OIDC, préférences (fuseau horaire) | 1 |
| `gamification` | XP, streaks, succès | 1 |
| `group` | groupes, membres, invitations, réglages, classements | 2 |
| `live` | parties synchronisées en temps réel | 3 |
| `feedback` | signalements d'erreurs dans le contenu (ADR 0042) | 3 ter |
| `ranked` | classé : cotes Glicko-2 des joueurs et des cartes, rangs, duels, saisons (ADR 0046) | 3 ter |
| `notification` | rappels par notification Web Push, sur acceptation (ADR 0047) | 3 ter |
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
- Front : seulement les tokens du design system (ADR 0049, 0058), jamais la palette par défaut de Tailwind, ni dégradé, ni émoji (un test le vérifie) ; les boutons passent par `button()` ; on tutoie l'utilisateur. Un texte a 4,5 de contraste avec son fond dans les deux thèmes (un test le calcule), et un lien dans une phrase est souligné. Un écran qui charge, échoue ou n'a rien à montrer passe par `LoadingState`, `ErrorState` et `EmptyState` (un test le vérifie). Une animation répond à une action, dure peu, et porte son garde-fou `motion-safe:` (un test le vérifie).

### 4.4 API
- REST, ressources au pluriel, codes HTTP corrects, pagination bornée côté serveur.
- **Code-first** (voir ADR 0003) : la spécification OpenAPI est générée depuis les ressources annotées par smallrye-openapi, exportée et versionnée dans le dépôt (`backend/src/main/openapi/` ou équivalent), puis sert à générer le client TypeScript du frontend. Toute évolution de l'API apparaît dans le diff de la spec, que la CI vérifie à jour. Le client front est généré, jamais écrit à la main.
- Versioning par URI (`/api/v1/...`) dès le départ.
- Les endpoints qui modifient la progression (enregistrer une réponse) sont **idempotents** (identifiant de requête côté client) pour supporter les retries sur mobile.

### 4.5 Sécurité
- Toutes les routes sauf la santé, le contenu public (catalogue, Tour) et l'essai sans compte (ADR 0050) nécessitent un JWT valide.
- Autorisation vérifiée **côté serveur pour chaque ressource** (un utilisateur ne lit que sa progression, un membre ne voit que ses groupes).
- Chacun peut télécharger ses données et supprimer son compte ; un module qui garde des données personnelles implémente `PersonalData` (ADR 0056).
- Secrets uniquement en variables d'environnement, jamais dans le dépôt.
- Dépendances surveillées (Renovate ou Dependabot), scan des images en CI.
- En-têtes de sécurité et CORS configurés explicitement ; Content-Security-Policy et limite de débit des requêtes sans compte dans nginx (ADR 0054).

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

## 5. Contenu : des documents de référence aux cartes

### 5.1 Format de la source
Chaque sujet a son document de référence, `content/source/<sujet>-entretien-reference.md` (par exemple `docker-entretien-reference.md`), tant que le sujet n'est pas migré vers son dossier de notions (5.5, ADR 0026). Tous suivent le même format :
- Titre du document : `# <Sujet> : le document de référence pour les entretiens techniques`, suivi d'une introduction (ignorée par l'import).
- Chapitres : un titre `# N. TITRE` encadré de lignes de séparation `═══...`.
- Dans chaque chapitre, des sections `### Vue d'ensemble`, `### Cours`, `### En situation`, `### Questions techniques`, `### Questions de situation`, et facultativement `### QCM`.
- **Questions techniques** : une ligne `**Q : question ?** R : réponse.` Parfois la question contient un bloc de code juste après `**Q :**` et la réponse suit sur la ligne `R : ...` (chapitre des questions pièges, reconnu à son titre « QU'AFFICHE CE CODE ? » et non à son numéro).
- **Questions de situation** : `**Q : "énoncé"**` puis `R : "réponse rédigée"` sur plusieurs lignes, parfois avec des blocs de code, jusqu'à la question suivante ou la fin du chapitre.
- **QCM** (section facultative `### QCM`) : `**Q : question ?**`, puis de 2 à 8 choix `- [ ] faux` / `- [x] juste` (plusieurs bonnes réponses permises), puis `R : explication` (ADR 0019).
- Le glossaire (tableau Markdown) peut devenir un deck de cartes "définition".
- Les blocs de code indiquent leur langage (` ```go `, ` ```java `...) pour la coloration syntaxique.
- Les sections hors chapitre numéroté (méthode générale, récapitulatif final) sont ignorées par l'import.
- Le guide de rédaction d'un nouveau document est `content/README.md` : format, règles d'écriture d'une bonne question, liste de contrôle avant la pull request.

### 5.2 Pipeline d'import
- Un **outil d'import** (module `content`, exécutable en ligne de commande ou en tâche d'admin) parse la source et produit des cartes.
- **L'import se fait sujet par sujet** : importer Go ne touche ni aux cartes ni aux thèmes de Java. Les cartes retirées sont celles du sujet importé qui ont disparu de son document, jamais celles d'un autre sujet.
- Le parseur est du code **pur et entièrement testé unitairement** (fixtures Markdown dans les tests), car c'est la partie la plus fragile.
- Chaque carte reçoit un **identifiant stable** (dérivé du **sujet**, du chapitre et d'un hash normalisé de la question), pour que la progression des utilisateurs survive aux réimports et aux corrections de texte. Deux sujets peuvent avoir un chapitre 1 ou une question identique sans collision. Une modification de texte crée une **nouvelle version** de la carte, pas une nouvelle carte.
- L'import est **idempotent** : relancer l'import ne crée aucun doublon.
- Un rapport d'import, par sujet, liste les cartes ajoutées, modifiées, retirées, les doublons et les lignes non reconnues.
- **Contrôle du contenu en CI** : chaque pull request parse tous les documents de `content/source/` et échoue sur une anomalie (question sans réponse, doublon, en-tête invalide, fichier mal nommé). Une erreur de contenu se voit avant le merge, pas en production.
- À terme, le contenu pourra être migré vers un format plus structuré (un fichier YAML ou Markdown par carte dans `content/`), relu par pull request. Ne pas le faire en V1, mais garder le parseur isolé derrière une interface pour pouvoir changer de source.

### 5.3 Types de cartes
| Type | Origine | Évaluation |
|---|---|---|
| `FLASHCARD` | questions techniques | correction par le serveur sur ses choix (ADR 0051) ; auto-évaluation tant qu'elle n'en a pas (voir 6.1) |
| `CODE_OUTPUT` | questions pièges avec code (chapitre « qu'affiche ce code ? ») | correction sur ses choix, dont la CI vérifie le bon (ADR 0051) ; auto-évaluation tant qu'il n'en a pas |
| `MULTIPLE_CHOICE` | section `### QCM` des chapitres | correction automatique par le serveur (ADR 0019) |
| `OPEN_SCENARIO` | questions de situation | correction sur ses choix (ADR 0051) ; sans eux, rédaction libre, comparaison avec la réponse modèle et auto-évaluation |
| `DEFINITION` | glossaire | correction sur ses choix (ADR 0051) ; auto-évaluation tant qu'elle n'en a pas |
| `TRUE_FALSE` | fichiers de notion (`true-false`) | correction automatique : un QCM à deux choix fixes (ADR 0027) |
| `BUG_FIX` | fichiers de notion (`bug-fix`) | correction sur ses choix (ADR 0051) ; sans eux, rédaction de la correction puis auto-évaluation (ADR 0027) |
| `FILL_IN` | fichiers de notion (`fill-in`) | correction automatique de chaque trou, aux espaces près (ADR 0027) |
| `CODING_EXERCISE` | dossiers d'exercice (`exercices/`) | jugé par ses tests dans le bac à sable (ADR 0029, 0030) |

Modéliser le type avec une interface `sealed` côté domaine.

Le type `CODING_EXERCISE` porte les exercices de code d'un dossier de sujet (`exercices/`, ADR 0029) : on les essaie, puis on les rend, et leurs tests les jugent dans le bac à sable (ADR 0030).

### 5.4 En-tête des documents (le sujet)
Chaque document commence par un **en-tête** qui décrit son sujet (ADR 0013). Il n'y a pas de fichier de manifeste séparé : un document se suffit à lui-même, et l'import par un admin n'a besoin que de lui.

```
---
subject: go           # slug, identifiant stable : il entre dans l'identité des cartes, ne jamais le changer
title: Go
category: languages   # languages, frameworks, cloud-devops, data, security, fundamentals, ai
position: 20          # ordre d'affichage dans le catalogue (de 10 en 10, pour pouvoir intercaler)
status: published     # draft (importé mais invisible) ou published
---
```

Les commentaires ci-dessus sont là pour l'explication : un vrai en-tête n'a qu'une ligne `clé: valeur` par clé.

- Toutes les clés sont obligatoires ; une clé ou une valeur inconnue fait échouer l'import (et la CI).
- Le fichier s'appelle `<slug>-entretien-reference.md` : la CI vérifie la correspondance.
- Le slug d'un sujet est **définitif** : le changer change l'identité de toutes ses cartes et fait perdre la progression.
- **Ajouter un sujet** = une pull request de contenu :
  1. écrire le document en suivant `content/README.md` (à venir, étape 5 de la phase 1 bis) ;
  2. le mettre en `status: draft` ;
  3. CI verte ;
  4. merge et import ;
  5. relecture dans l'application ;
  6. une seconde PR le passe en `published`.

### 5.5 Refonte du contenu : référentiel, notions, niveaux (phase 3 bis, ADR 0025)
L'application doit servir trois publics avec le même contenu : **apprendre** un langage sans le connaître, **préparer** un entretien, **entretenir** un niveau d'expert. Les documents d'entretien actuels ne servent bien que le deuxième. La refonte change l'unité de contenu :
- **Un référentiel par sujet** (`content/<sujet>/referentiel/*.yaml`) : l'inventaire de **toutes** les notions du sujet, en domaines → chapitres → notions. Chaque notion a un identifiant définitif, un **niveau** (`beginner`, `intermediate`, `expert`), une **priorité** (1 essentiel, 2 courant, 3 avancé, 4 rare) et ses **prérequis**. La CI vérifie le référentiel (identifiants uniques, prérequis existants, sans cycle) et mesure la couverture du contenu par priorité.
- **Une notion = une courte leçon + des cartes de formats variés** : cours, vrai ou faux, QCM, compléter le code, qu'affiche ce code, bug à corriger, mise en situation, définition, et des **exercices de code** (un ou plusieurs fichiers, jugés par des tests). Une notion du Tour a en plus ses pages, écrites pour découvrir (ADR 0037).
- **Matrice** : **4 à 5 cartes par format** pour chaque notion, sur les formats qui ont du sens pour elle (pas de « bug à corriger » sur l'histoire du langage), et au moins un exercice de code par chapitre.
- **Production par priorité** : toutes les notions de priorité 1, puis 2, etc. Claude rédige les brouillons (référentiel, cartes, exercices) à partir des documents existants ; **chaque PR de contenu est relue par le propriétaire du projet avant le merge** (ADR 0025).
- **Le code est vérifié** : la CI exécute chaque « qu'affiche ce code » et compare la sortie à la réponse ; la solution modèle de chaque exercice doit passer ses tests, et le code de départ y échouer.
- **Un sujet, c'est un langage ou un outil** : le sujet Java couvre le langage, la JVM, la bibliothèque standard et l'outillage Java (tests, build). Les chapitres du document actuel qui relèvent d'autres sujets du catalogue (Spring, Quarkus, Docker, Kubernetes, SQL, Kafka, Git, sécurité, cloud, algorithmes, conception, entretien comportemental) deviennent leurs propres sujets lors de la migration.

Un sujet migré a son **dossier** `content/<sujet>/` (`subject.yaml`, référentiel, un fichier YAML par notion, cartes à clé explicite, ADR 0026) ; les documents de `content/source/` restent la source des autres sujets.

### 5.6 Feuille de route du contenu
Les sujets s'ajoutent un par un, au rythme de la rédaction, indépendamment des phases de développement (section 9). Ordre indicatif :

| Sujet | Catégorie | État |
|---|---|---|
| Java | Langages | ✅ rédigé, migré vers son dossier de notions (ADR 0026), complet : 347 notions, 7 072 cartes, 39 exercices |
| Go | Langages | ✅ rédigé, migré vers son dossier de notions (ADR 0026), complet : 223 notions, 4 893 cartes, 21 exercices jugés dans le bac à sable Go (ADR 0033) |
| Spring Boot, Quarkus, Jakarta EE, JPA & Hibernate | Frameworks | sortis du document Java (ADR 0025), en brouillon jusqu'à leur réécriture en dossier de notions (ADR 0050) |
| SQL, Kafka, Cache | Données | sortis du document Java, en brouillon jusqu'à leur réécriture |
| Docker, Kubernetes, CI/CD, Observabilité, Cloud, Git | Cloud & DevOps | sortis du document Java, en brouillon jusqu'à leur réécriture |
| API REST, Réseau & Linux, Algorithmes, Conception, Entretien comportemental | Fondamentaux | sortis du document Java, en brouillon jusqu'à leur réécriture |
| Sécurité | Cybersécurité | sortie du document Java, en brouillon jusqu'à sa réécriture |
| C++ | Langages | référentiel écrit, en brouillon : 335 notions en 16 domaines, pour C++23 et GCC 14 (ADR 0053) ; les domaines « Bases du langage » et « Types composés » rédigés en priorité 1 (38 notions, 920 cartes, 35 pages du Tour, 9 exercices) ; la suite par priorité, un chapitre par PR |
| Python, TypeScript, Kotlin, C#, C, Rust | Langages | annoncés (un dossier sans contenu, ADR 0052), à rédiger |
| IA : machine learning, LLM & prompting | IA | à rédiger |
| Programmer de zéro | Fondamentaux | ✅ rédigé en brouillon, en Java 25 (ADR 0039) : 23 notions, 115 cartes, un Tour de 23 pages ; à relire dans l'application avant de le publier |

---

## 6. Règles métier

### 6.1 Révision
- Une **session** propose les cartes **dues** (selon la planification) puis de nouvelles cartes, dans la limite d'un nombre quotidien de nouvelles cartes réglable.
- Une session porte sur **un sujet**, pour une **durée** choisie (5, 10, 15 ou 20 minutes, ou sans limite), avec toutes les questions ou seulement celles que corrige le serveur (ADR 0048). La durée borne la session sans presser la réponse : la carte en cours n'est jamais coupée.
- **Chaque question se corrige** : on choisit parmi des réponses propres à la carte, mélangées à chaque affichage, et le serveur corrige ; juste, la carte est retrouvée (Bien), fausse, elle est à revoir (ADR 0051). Personne ne se note soi-même.
- Le temps de la migration (ADR 0051), une carte qui n'a pas encore ses choix s'auto-évalue : l'utilisateur voit la question, réfléchit (ou écrit sa réponse), **retourne la carte**, puis répond **« Je savais »** ou **« Je ne savais pas »** : les notes Bien et À revoir de FSRS (ADR 0048 ; l'API accepte aussi Difficile et Facile). Au clavier, Espace retourne la carte, 1 et 2 répondent ; sur un écran tactile, on la glisse à droite si l'on savait, à gauche sinon (ADR 0050).
- Les cartes proposées viennent des **sujets suivis** par l'utilisateur ; filtrage possible par sujet, par thème (chapitre) et par type de carte.
- Le quota quotidien de nouvelles cartes est **propre à chaque sujet** (ADR 0048) : finir sa session de Java ne prive pas Go de ses nouvelles cartes. Dans un sujet, les cartes dues passent toujours en premier.
- Une seule carte d'une même notion par jour : ses sœurs attendent le lendemain, sans que leur état FSRS change (ADR 0031).
- Chaque sujet suivi a son **parcours** (ADR 0032) : *apprendre* (seulement les notions dont les prérequis sont acquis, du débutant vers l'expert), *préparer un entretien* (par priorité, le défaut) ou *entretenir son niveau* (intermédiaire et expert d'abord). Il range les nouvelles cartes, jamais les dues. Qui apprend découvre une notion dans le Tour avant de la réviser : une notion du Tour attend d'y être validée (ADR 0059).
- Sous une réponse fausse, « Revoir la leçon » ; et par sujet, un carnet des cartes ratées dans la semaine, à refaire sans rien enregistrer (ADR 0057).
- Arrêter de suivre un sujet ne supprime pas la progression : ses cartes ne sont plus proposées, et elles reviennent si on le suit de nouveau.

### 6.2 Répétition espacée : FSRS
- Utiliser l'algorithme **FSRS** (celui des versions récentes d'Anki). État par couple (utilisateur, carte) : stabilité, difficulté, date d'échéance, nombre de révisions, nombre d'oublis, état (nouvelle, en apprentissage, en révision, réapprentissage).
- L'algorithme est implémenté (ou intégré via une bibliothèque, à évaluer en phase 1 et à consigner dans un ADR) dans le **domaine**, en Java pur, avec des tests qui vérifient les intervalles produits sur des scénarios connus.
- Chaque réponse est enregistrée dans un **journal de révision** append-only (permettra de réentraîner les paramètres FSRS et de produire des statistiques).

### 6.3 Gamification
- **XP** (ADR 0035, elle a remplacé les points de la phase 1) : attribuée à chaque révision, quelle que soit la note, selon l'effort du format (un exercice de code vaut quatre questions de cours), avec un bonus pour une réussite corrigée par le serveur. Chaque sujet a son XP et son niveau. Stockée comme des **événements** dans un registre append-only (`points_ledger`, qui a gardé son nom) ; un total est une somme ou une projection, jamais un compteur modifié en place.
- **Streak** : un jour compte si l'utilisateur a terminé au moins un objectif minimal (par exemple 10 cartes) **dans son fuseau horaire**. Un jour manqué remet le streak à zéro, sauf s'il est couvert par un **gel de streak** (ADR 0036) : chaque série de 7 jours en rapporte un, 2 au plus en réserve, consommé automatiquement un jour manqué.
- La gamification ne doit jamais pousser à bâcler : pas d'XP pour aller plus vite en révision personnelle.
- **Gamification v2** (phase 3 ter, ADR 0025) : carte des compétences par sujet, **maîtrise par notion** (mémoire FSRS et diversité des formats réussis), XP par sujet (plus pour les formats exigeants, jamais pour la vitesse), quêtes du jour et de la semaine qui font varier les formats, succès, gel de streak.
- **Partage** : cartes de partage (chapitre maîtrisé, podium, promotion) et **profil public de compétences**, **privé par défaut** (RGPD, protection des données dès la conception), que chacun rend public ou privé à tout moment. Le profil distingue le **vérifié** (classé, live, défis, exercices jugés par le serveur) du **déclaré** (maîtrise en révision personnelle, auto-évaluée).
- **Classé** : un **rang par sujet** (Bronze → Argent → Or → Platine → Diamant → Maître, en divisions), cote Glicko-2 cachée, matchs de placement, saisons aux récompenses cosmétiques ; duels asynchrones d'abord, classé en direct ensuite ; uniquement des formats corrigés par le serveur, questions adaptées au niveau (cote de difficulté des cartes).

### 6.4 Groupes (phase 2)
- Un créateur crée un groupe, génère un lien d'invitation (expirant), définit : sujets et thèmes inclus, types de cartes, objectif quotidien, visibilité du classement (nominatif, anonymisé, désactivé).
- Classement hebdomadaire basé sur la régularité et l'XP.
- Défis de groupe : une série de cartes commune, avec un délai de réponse par carte.

### 6.5 Mode live (phase 3)
- Partie synchronisée façon quiz, sur un sujet choisi par l'hôte : un hôte lance, les membres rejoignent, chaque question a un chrono court, points selon la justesse et la rapidité, classement en direct.
- Uniquement des cartes à correction automatique (`MULTIPLE_CHOICE`, `CODE_OUTPUT` en QCM).

### 6.6 Le Tour (phase 3 quater, ADR 0037)
- Deux publics : quelqu'un qui programme déjà dans un autre langage suit le Tour de ce langage, au niveau du Go Tour ; quelqu'un qui n'a jamais programmé suit « Programmer de zéro ».
- Une page appartient à une notion : un texte pour découvrir, un exemple à modifier et à exécuter, et parfois un « À toi » dont le serveur compare la sortie à celle attendue. L'ordre est celui du référentiel.
- Sur un écran large, la page a deux colonnes : le cours à gauche, avec « Teste-toi » (des questions de la notion à retourner), le code à droite (ADR 0050).
- Une notion se valide à la fin de sa dernière page, en répondant juste à 2 ou 3 de ses questions, corrigées par le serveur ; elle reste validée. Ces réponses ne sont pas enregistrées comme des révisions : FSRS ne voit que les vraies révisions, espacées.
- Pour qui apprend, valider une notion dans le Tour l'ouvre à la révision (ADR 0059).
- En fin de chapitre, ses exercices ; depuis chaque page, « S'entraîner » ouvre toutes les cartes de la notion, corrigées par le serveur quand leur format le permet, sans rien enregistrer (ADR 0038).
- Chaque langage a son Tour, fait de contenu seulement ; un nouveau langage demande aussi son image de bac à sable.

---

## 7. Modèle de données (première ébauche, à affiner)

Tables principales de la V1 (noms en anglais, `snake_case`, identifiants UUID v7 ou séquences, à décider en ADR) :
- `subject` (id stable, slug unique, title, category, position, status, created_at) : un sujet du catalogue (2.5)
- `theme` (id, subject_id, slug, title, position) : slug unique **au sein du sujet**
- `card` (id stable, theme_id, type, created_at, retired_at)
- `card_version` (id, card_id, version, question, answer, payload JSON pour les choix ou le code, created_at)
- `app_user` (id, oidc_subject unique, display_name, timezone, created_at)
- `followed_subject` (user_id, subject_slug, followed_at) : les sujets suivis, qui alimentent les révisions (ADR 0014)
- `review_state` (user_id, card_id, stability, difficulty, due_at, reps, lapses, state, last_review_at) : clé primaire (user_id, card_id)
- `review_log` (id, user_id, card_id, card_version_id, rating, reviewed_at, elapsed_ms, request_id unique)
- `points_ledger` (id, user_id, amount, reason, subject_slug, source_id, occurred_at) : le registre de l'XP (ADR 0035)
- `daily_activity` (user_id, local_date, cards_reviewed, goal_reached) : base du calcul du streak

Phases suivantes : `study_group`, `group_member`, `group_settings`, `group_invitation`, `challenge`, `live_session`, `organization`.

Toute évolution du schéma passe par une migration Flyway versionnée, rétrocompatible avec la version du code précédente (ajouter avant de supprimer).

---

## 8. Stratégie de tests

| Niveau | Ce qu'on teste | Outils |
|---|---|---|
| Unitaire (le plus nombreux) | domaine : FSRS, calcul des streaks, XP, parseur Markdown (sur des extraits de chaque document de référence) | JUnit, AssertJ |
| Intégration | repositories, migrations, endpoints REST, sécurité (accès refusé aux ressources d'autrui) | `@QuarkusTest`, REST Assured, PostgreSQL réel via Dev Services/Testcontainers |
| Architecture | règles de dépendances entre couches et modules | ArchUnit |
| Frontend | composants et hooks | Vitest, Testing Library |
| Bout en bout | quelques parcours critiques (se connecter, essayer sans compte, réviser une session, voir son streak), l'accessibilité des pages principales dans les deux thèmes, et la vitesse des pages publiques sur un téléphone lent simulé | Playwright, axe-core |

Règles : chaque bug corrigé reçoit un test qui le reproduit ; pas de `Thread.sleep` dans les tests ; le temps est piloté par un `Clock` fixe ; la couverture est un indicateur, pas un objectif.

---

## 9. Feuille de route

Chaque phase se termine par une démo fonctionnelle, une CI verte, la documentation à jour et une rétrospective courte (ce qui a marché, ce qu'on change).

### Phase 0 : fondations ✅
- Dépôt monorepo : `backend/`, `frontend/`, `content/`, `docs/`, `infra/` (compose local).
- Projet Quarkus, structure de modules, premier test ArchUnit, Spotless, profil de dev avec PostgreSQL et Keycloak en Dev Services.
- Projet React + TypeScript + Vite, lint, tests, PWA minimale.
- CI GitHub Actions (build + tests backend et frontend).
- ADR 0001 (monolithe modulaire) et ADR 0002 (stack).
- **Terminé quand** : `./mvnw quarkus:dev` et `npm run dev` lancent tout en local, un endpoint `/api/v1/health` est appelé par le front, la CI est verte.

### Phase 1 : MVP révision solo
1. ✅ Parseur et import du contenu (avec rapport), consultation des thèmes et cartes.
2. ✅ Authentification OIDC de bout en bout, création du profil au premier login.
3. ✅ Session de révision avec auto-évaluation et planification FSRS.
4. ✅ Points, objectif quotidien et streak.
5. ✅ Tableau de bord personnel (cartes dues, streak, progression par thème).
6. ✅ Parcours de bout en bout (Playwright) et rétrospective.
7. Déploiement sur l'hébergeur choisi, observabilité de base : **après l'étape 1 de la phase 1 bis** (voir ci-dessous).
- **Terminé quand** : quelques testeurs peuvent réviser chaque jour sur mobile ; on mesure combien reviennent après 7 jours.

### Phase 1 bis : catalogue multi-sujets ✅
Faire de Recallio un catalogue de sujets (2.5), avec Java et Go comme premiers sujets.

**L'étape 1 doit précéder le premier déploiement** : elle change l'identité des cartes (le sujet y entre). Aujourd'hui, sans utilisateur réel, ce n'est qu'un réimport ; après le déploiement, cela ferait perdre leur progression aux testeurs.

1. ✅ **Sujets dans le modèle et import par sujet**.
   - Contenu : table `subject`, en-tête de document (5.4), thèmes rattachés à un sujet (slug préfixé par le sujet).
   - Identité : identifiants des thèmes et des cartes préfixés par le sujet.
   - Import : retraits limités au sujet importé, chapitre des questions pièges reconnu à son titre ; import de Java et de Go.
   - ADR 0013 : modèle multi-sujets et en-tête des documents.
2. ✅ **Catalogue par sujet (API et PWA)**.
   - API : `GET /api/v1/subjects`, puis les thèmes et les cartes d'un sujet ; brouillons visibles des seuls admins.
   - PWA : pages *Sujets* → *Thèmes* → *Cartes*, regroupées par catégorie ; coloration syntaxique selon le langage du bloc de code.
3. ✅ **Sujets suivis**.
   - Choix des sujets au premier login (écran d'accueil) et dans le profil.
   - File de révision limitée aux sujets suivis, filtre par sujet, quota de nouvelles cartes réparti entre eux.
4. ✅ **Tableau de bord par sujet** : progression par sujet, puis par thème dans chaque sujet.
5. ✅ **Outillage du contenu**.
   - `content/README.md` (guide de rédaction et liste de contrôle).
   - Contrôle du contenu en CI (tous les documents parsés, échec sur anomalie).
   - Écran admin des brouillons (le catalogue lui-même : un admin y voit les brouillons, badge « Brouillon », depuis l'étape 2), et documentation de la procédure « ajouter un sujet » (`content/README.md`, section 7). Job CI `content` dédié au contrôle des documents.
- **Terminé quand** : un utilisateur suit Java et Go, révise les deux dans la même session et voit sa progression par sujet ; ajouter un troisième sujet ne demande qu'une pull request de contenu.

Ensuite, chaque nouveau sujet (5.5) est une pull request de contenu, qui peut avancer en parallèle des phases suivantes.

### Phase 2 : groupes ✅
Groupes, invitations, réglages (dont les sujets du groupe), classements (Valkey/Redis introduit ici si nécessaire), défis de groupe, cartes QCM.

1. ✅ **Groupes et membres** : créer un groupe, voir ses groupes et leurs membres, quitter, retirer un membre, supprimer (ADR 0015).
2. ✅ **Invitations** : lien d'invitation expirant, rejoindre un groupe par ce lien (ADR 0016).
3. ✅ **Réglages du groupe** : sujets et thèmes inclus, types de cartes, objectif quotidien, visibilité du classement (nominatif, anonymisé, désactivé) ; réviser avec le groupe.
4. ✅ **Classement hebdomadaire** : régularité et points (en SQL ; Valkey seulement si un besoin réel apparaît). Calculé par le module `gamification` (ADR 0018).
5. ✅ **Cartes QCM** (`MULTIPLE_CHOICE`) : format dans les documents, correction automatique (ADR 0019).
6. ✅ **Défis de groupe** : une série de QCM commune, avec un délai de réponse par carte mesuré par le serveur (ADR 0020).
- **Terminé quand** : une équipe crée son groupe, invite ses membres, révise les mêmes sujets et suit son classement de la semaine.

### Phase 3 : mode live ✅
WebSockets, parties synchronisées, chrono, classement en direct, tests de charge simples.

1. ✅ **Salle d'attente en temps réel** : WebSockets Next authentifié par le JWT (sous-protocole), tout membre ouvre une partie sur un sujet du groupe, les membres la rejoignent et se voient en direct (ADR 0021).
2. ✅ **Déroulé synchronisé** : l'hôte démarre ; la même question pour tous, au même moment, chrono commun mesuré par le serveur ; correction révélée, puis question suivante ; reprise après reconnexion (ADR 0022).
3. ✅ **Points et classement en direct** : justesse et rapidité, classement après chaque question, podium final, résultats conservés (ADR 0023).
4. ✅ **Robustesse et charge** : départ de l'hôte, déconnexions, parties abandonnées, limites ; test de charge simple et métriques (ADR 0024).
- **Terminé quand** : les membres d'un groupe jouent ensemble une partie live de bout en bout, sur mobile, et le classement s'affiche en direct.

### Phase 3 bis : refonte du contenu (ADR 0025) ✅
Faire du contenu un référentiel complet, pour apprendre, préparer un entretien ou entretenir son niveau (5.5).

1. **Référentiel et modèle** : ✅ le référentiel Java (toutes les notions, niveaux, priorités, prérequis) vérifié en CI ; ✅ découpage des chapitres hors Java en sujets propres ; ✅ dossier de sujet, un fichier YAML par notion, cartes à clé explicite (ADR 0026) ; ✅ migration sans perte des cartes Java vers les notions.
2. ✅ **Nouveaux formats texte** : vrai ou faux, bug à corriger, compléter le code (ADR 0027).
3. ✅ **Contrôles du contenu en CI** : matrice par notion et couverture du référentiel par priorité, publiées dans le résumé du job `content` (non bloquantes jusqu'au pilote) ; exécution de chaque « qu'affiche ce code » avec JShell, comparée à sa vérification `check` (ADR 0028).
4. ✅ **Exercices de code** : ✅ format en dossier (énoncé, fichiers de départ, solution, tests), import et consultation, auto-évalués en attendant le bac à sable (ADR 0029) ; ✅ **bac à sable d'exécution** (avancé depuis la phase 5 : service `runner` et conteneurs jetables, ADR 0030) et vérification de chaque exercice en CI ; ✅ correction par les tests en révision (essayer, puis rendre ; ADR 0030) ; ✅ éditeur multi-fichiers dans la PWA (CodeMirror 6, un onglet par fichier, chargé à la demande).
5. ✅ **Parcours** : ✅ un parcours par sujet suivi, apprendre (prérequis, déblocage), préparer un entretien, entretenir son niveau (ADR 0032) ; ✅ leçons avant la première carte et carte du parcours ; ✅ deux cartes d'une même notion pas le même jour (ADR 0031).
6. ✅ **Pilote** : deux ou trois chapitres Java complets en priorité 1, pour valider la matrice et la qualité des brouillons (✅ « Premiers pas », « Types et variables » et « Opérateurs », 20 notions déclarées complètes et vérifiées en CI ; bilan dans `docs/retro/pilote-contenu.md`) ; puis la production (✅ Java : toutes les notions complètes, toutes priorités, 4 cartes par format ; bilan dans `docs/retro/production-java.md`).
- **Terminé quand** : un débutant suit le parcours Java de bout en bout sur les notions de priorité 1, en codant dans l'application.

### Phase 3 ter : gamification v2, partage, classé (ADR 0025) ✅
Carte des compétences, maîtrise par notion, XP, quêtes, succès, gel de streak ; indice de préparation, examen blanc, signalement des erreurs, rappels ; cartes de partage et profil public (privé par défaut) ; classé par sujet : duels asynchrones, rangs, placement, cotes des cartes, puis saisons et classé en direct.

**Gamification v2, en révision personnelle**
1. ✅ **Maîtrise par notion** : nouvelle, découverte, acquise, maîtrisée, experte, d'après la mémoire FSRS et les formats réussis ; donnée par la carte du parcours, avec ce qui mène au niveau suivant (ADR 0034).
2. ✅ **Carte des compétences** (PWA) : la page du parcours range les notions par domaine, chacun avec la barre de maîtrise de ses notions, et le domaine où l'on en est se déplie d'emblée ; chaque notion dit ce qui mène au niveau suivant ; un résumé par sujet sur le tableau de bord.
3. ✅ **XP par sujet** (ADR 0035) : l'XP remplace les points ; une XP de base à chaque révision, quelle que soit la note, et un bonus pour une réussite corrigée par le serveur, plus fort pour les formats exigeants ; jamais liée au temps. Un niveau par sujet, sur le tableau de bord.
4. ✅ **Gel de streak** (ADR 0036) : gagné avec le streak, consommé automatiquement un jour manqué.

**Le Tour** (phase 3 quater, ci-dessous) **s'intercale ici** : les quêtes et les succès pourront le récompenser (« lire 3 pages », « finir le chapitre Concurrence »).

5. ✅ **Quêtes du jour et de la semaine** (ADR 0040) : trois par jour, deux par semaine, calculées pour chaque utilisateur ; elles font varier les formats et récompensent le Tour, jamais la vitesse, et rapportent leur XP une fois.
6. ✅ **Succès**, débloqués une seule fois (ADR 0040) : régularité, exercices, sujets, Tour et quêtes ; sur le profil.

**Préparation et qualité**

7. ✅ **Indice de préparation** par sujet (ADR 0041) : la part des notions essentielles (priorité 1) acquises et maîtrisées, comme le « prêt pour l'examen » d'Ornikar ; prêt à partir de 80 %, sur le tableau de bord.
8. ✅ **Examen blanc** : une simulation d'entretien chronométrée, sur des formats corrigés par le serveur, avec une note et le corrigé à la fin (ADR 0044) : 20 questions au plus, une minute chacune, les notions essentielles d'abord ; rien dans FSRS.
9. ✅ **Signaler une erreur** sur chaque carte et chaque page du Tour : le contenu rédigé par Claude aura des erreurs, les utilisateurs doivent pouvoir les remonter (ADR 0042, module `feedback`) ; les admins les traitent depuis leur profil.
10. ✅ **Rappels** : des notifications de la PWA, sur acceptation, pour garder l'habitude (« Ta série de 12 jours t'attend ») ; Web Push implémenté avec le JDK (RFC 8291, 8292), un rappel par jour au plus, à l'heure choisie, si l'objectif n'est pas atteint (ADR 0047, module `notification`).

**Partage**

11. ✅ **Profil public de compétences**, privé par défaut ; le vérifié séparé du déclaré (ADR 0043) : une adresse choisie, `/u/<adresse>`, lisible sans compte.
12. ✅ **Cartes de partage** : chapitre maîtrisé, podium, promotion (ADR 0045) : une image dessinée dans le navigateur, confiée au partage du téléphone ; la promotion arrive avec le classé.

**Classé** (ADR 0046, module `ranked`)

13. ✅ **Cote de difficulté des cartes**, sur les réponses corrigées par le serveur : une cote Glicko-2 par carte, qui apprend des révisions et des duels.
14. ✅ **Cote et rang par sujet, duels asynchrones** (backend) : cote Glicko-2 cachée, rangs en divisions, matchs de placement ; 5 questions près du niveau, 30 secondes chacune mesurées par le serveur.
15. ✅ **Duels dans la PWA** : la page « Classé » (rangs, placement, insignes, derniers duels) et le duel ; la promotion se partage.
16. ✅ **Saisons** : un trimestre civil ; la cote se rapproche de 1500 de moitié, le placement recommence, le meilleur rang reste en insigne.
17. ✅ **Classé en direct** : une file par sujet, deux joueurs présents appariés dans un duel joué en même temps, suivi par interrogation du serveur.
- **Terminé quand** : un utilisateur suit sa maîtrise notion par notion, gagne de l'XP et des succès, sait s'il est prêt pour son entretien, partage un profil de compétences, et monte de rang en duel sur un sujet.

### Phase 3 quater : le Tour, apprendre un langage (ADR 0037) ✅
Apprendre un langage pas à pas, comme sur le Go Tour, et valider chaque page, comme sur Ornikar (6.6). Elle passe entre les étapes 4 et 5 de la phase 3 ter.

1. ✅ **Le mode « programme » du runner** : compiler et lancer un programme, renvoyer ses sorties et son code de sortie, dans le même bac à sable ; en Go, tout paquet standard est permis (index des modules désactivé).
2. ✅ **Le format des pages** : le bloc `tour` des notions (texte, exemple, « À toi »), leur import, et la CI qui joue chaque exemple et chaque « À toi » dans le bac à sable ; les pages du premier chapitre Go (6 pages, 3 « À toi »), pour éprouver le format.
3. ✅ **L'API du Tour** : le sommaire et les pages d'un sujet, publics comme le catalogue, et `POST /api/v1/playground-runs`, une exécution à la fois par utilisateur, exercices compris.
4. ✅ **Le Tour dans la PWA** : le sommaire et la page (texte, éditeur, « Exécuter », sortie, précédent et suivant), sur mobile aussi ; lisible sans compte, « Exécuter » demande de se connecter.
5. ✅ **Valider une notion** : ✅ les questions de vérification, corrigées par le serveur, et les notions validées, marquées dans le sommaire ; ✅ la reprise où l'on s'est arrêté (« Reprendre le Tour ») ; ✅ « S'entraîner sur cette notion » : un entraînement libre, non enregistré, plutôt qu'un filtre de la file de révision qui contredirait l'ADR 0031 (ADR 0038).
6. ✅ **« À toi » et exercices** : la sortie de l'utilisateur comparée sur le serveur à celle attendue, qui ne quitte pas le backend (`POST /api/v1/me/tour/{s}/{notion}/tasks`) ; les exercices de fin de chapitre dans le sommaire et sur la dernière page du chapitre, ouverts dans l'entraînement de leur notion (ADR 0038).
7. ✅ **Le Tour Go** : une page par notion débutante, et les premières notions de concurrence (channels fermés, `select`, délais, pool de workers, pipelines, sémaphores) : 81 notions, 81 pages, 29 « À toi », toutes jouées dans le bac à sable par `TourTest`.
8. ✅ **Le Tour Java** : une page par notion débutante, de « Bienvenue » aux annotations : 106 notions, 106 pages, 19 « À toi », en fichiers source compacts Java 25 quand c'est possible, toutes jouées dans le bac à sable.
9. ✅ **Programmer de zéro** : le sujet (en brouillon), son référentiel de 23 notions, leurs leçons et leurs cartes, et son Tour, en Java 25 (ADR 0039).
10. ✅ **Parcours de bout en bout et rétrospective** : l'e2e lit, exécute, reprend, valide, s'entraîne et vérifie un « À toi » ; bilan dans `docs/retro/le-tour.md`.
- **Terminé quand** : quelqu'un qui programme dans un autre langage apprend Go ou Java dans le Tour, en exécutant chaque exemple et en validant chaque notion, et quelqu'un qui n'a jamais programmé suit « Programmer de zéro ».

### Avant le déploiement : retours du test local
Le propriétaire a testé l'application en local avant de la déployer ; ses retours passent avant le déploiement.

1. ✅ **Session de révision** : un sujet à la fois, une durée, un mode « questions corrigées », un quota de nouvelles cartes par sujet, et des cartes qui se retournent, avec « Je savais » ou « Je ne savais pas » (ADR 0048).
2. ✅ **Refonte du front**, pour une interface qui n'ait pas l'air générée et qui retienne les utilisateurs : la piste « Coach », choisie parmi trois (ADR 0049, 0050).
   1. ✅ **Design system** :
      - tokens sémantiques, clairs et sombres, à la place de la palette par défaut ;
      - Lexend et JetBrains Mono servies par l'application ;
      - icônes Phosphor à la place des émojis ;
      - une recette de bouton ;
      - un test qui garde ces règles (ADR 0049).
   2. ✅ **Navigation** : une barre en bas sur mobile, un menu sur ordinateur, le Tour dans « Apprendre » ; le profil sous son nom, la déconnexion dans le profil.
   3. ✅ **Accueil** à une seule action (« Continuer : Java · 10 min ») autour de l'anneau de l'objectif du jour, et une fin de session qui marque (célébration, XP gagnée, « Encore 5 minutes »).
   4. ✅ **Révision** dans le nouveau style : cartes, gestes et raccourcis clavier, verdict animé, sons et vibrations à activer dans le profil.
   5. ✅ **Le Tour en deux colonnes** : le cours à gauche, avec des questions à retourner (« Teste-toi ») ; le code à droite.
   6. ✅ **Essai sans compte** (cinq questions corrigées par le serveur, rien d'enregistré), et le temps par jour choisi à l'inscription : la première session part aussitôt.
   7. ✅ **Notifications et installation** de la PWA, demandées à la fin d'une session : notre question d'abord, celle du navigateur ensuite ; l'installation à partir de la deuxième visite.
   8. ✅ **Les autres écrans** dans le nouveau style, avec le tutoiement partout, messages d'erreur du backend compris.
   9. ✅ **Accessibilité et performances** contrôlées en CI : axe-core dans les parcours de bout en bout, la compression par nginx, un budget de 300 Ko pour le JavaScript de l'ouverture.
   10. ✅ **Mesure de la rétention** à 1, 7 et 30 jours, sur une page des admins.
3. ✅ **Sujets en brouillon** : les 19 sujets sortis du document Java ne sont visibles que des admins jusqu'à leur réécriture en dossiers de notions (ADR 0050).
4. **Des choix pour toutes les questions** (ADR 0051) : le serveur corrige chaque carte, plus d'auto-évaluation.
   1. ✅ **Pilote** : les choix sur tous les formats, mélangés à l'affichage, vérifiés en CI pour le code ; « Programmer de zéro » et le chapitre Java « Premiers pas » convertis.
   2. **Java** : toutes les cartes à convertir, au tutoiement.
   3. **Go**, puis la fin de l'auto-évaluation : choix obligatoires, mode « questions corrigées » retiré, défis, live, duels et examen blanc ouverts à tous les formats.
5. ✅ **Les pages Cours, Tour et Révisions** (ADR 0052) :
   - la navigation devient Accueil, Cours, Tour, Révisions et Jouer ;
   - chaque page s'ouvre sur les sujets par catégorie, avec leur logo, ceux en préparation en « work in progress » ;
   - le cours d'un sujet, leçon par leçon ;
   - la catégorie Cybersécurité, et les langages annoncés ;
   - un premier login guidé par le but, qui conduit qui apprend au Tour.
6. ✅ **Relier le Tour à la révision** (ADR 0059) : en parcours « apprendre », une notion qui a des pages dans le Tour n'entre en révision qu'une fois validée dans le Tour.
7. **Quatre nouveaux sujets complets**, prêts à l'emploi (référentiel, leçons, cartes à choix, exercices), livrés par priorité :
   1. **C++** : ✅ son bac à sable (GCC 14, doctest, ADR 0053) ; ✅ son référentiel (335 notions, dont 118 de priorité 1) ; les priorités 1 et 2, avec le Tour, un chapitre par PR (✅ les domaines « Bases du langage » et « Types composés » : 38 notions sur 118) ; les priorités 3 et 4.
   2. **Spring Boot**, puis **Quarkus** : des exercices jugés par `@SpringBootTest` et `@QuarkusTest`, dans un bac à sable qui embarque le framework.
   3. **Python** : son bac à sable, puis le contenu et le Tour.
8. **Retours de l'audit** du 2026-11-10 (`docs/audit-2026-11.md`, où chaque numéro ci-dessous est détaillé), une PR par étape :
   1. ✅ **Correctifs d'interface** (1 à 4) : débordement sur mobile, « API : UP », libellé du Tour, éditeur du Tour remonté sur mobile.
   2. ✅ **Qualité des cartes** (18 à 24) : règles vérifiées en CI, guide de rédaction, notion affichée sur la carte, pilote corrigé.
   3. ✅ **Poids du front** (12, 13) : routes chargées à la demande, coloration limitée aux langages utilisés.
   4. ✅ **Sécurité** (14 à 16) : Content-Security-Policy, limite de débit sans compte, ADR sur les réponses publiques.
   5. ✅ **Suppression de compte et export des données** (17) : chaque module répond de ses données par un port (ADR 0056).
   6. ✅ **Ergonomie** (5, 7 à 10) : filet d'erreur, icônes de la PWA, fraîcheur des données, chapitres repliés, profil en sections. L'accueil d'un nouveau venu (6) passe à l'étape 8.9.3, qui refait l'accueil.
   7. ✅ **Apprendre de ses erreurs** (27, 35) : « Revoir la leçon » sous une réponse fausse, et un carnet des erreurs de la semaine, à refaire sans rien enregistrer (ADR 0057). L'annulation de la dernière réponse (38) est abandonnée : elle contredit l'ADR 0051.
   8. **Avant la bêta** (25, 26, 34, 40, 44) : compte à rebours avant l'entretien, défi du jour, test de niveau, référencement, parcours mesuré.
   9. ✅ **Refonte « Rico »** (47 à 57) : la direction ludique retenue le 2026-11-10, en clair et en sombre, avec tout ce qui ne coûte que du temps. L'ADR 0058 la décide et révise l'ADR 0049 ; une PR par étape :
      1. ✅ **Identité** (47, 48) : couleurs, Nunito, boutons en relief, Rico, thèmes clair et sombre au choix dans le profil (ADR 0058).
         Les contrastes des tokens sont calculés à chaque `npm test`, et axe-core passe aussi dans le thème sombre.
      2. ✅ **Page publique** (49) : la première question de l'essai posée dans la page, puis les chiffres du catalogue, pour qui, comment ça marche, les sujets.
      3. ✅ **Accueil connecté** (50, 6) : le mot de Rico, la carte « Aujourd'hui » avec l'anneau du jour, le chemin des domaines du sujet, cinq semaines d'activité ; pour un nouveau venu, pas de bloc à zéro avant la première carte.
      4. ✅ **Carte de révision** (51) : de grands choix au contour net, un verdict qui ne repose jamais sur la seule couleur, Rico qui réagit, un focus visible partout, et le focus qui passe sur « Carte suivante » après « Valider ».
      5. ✅ **Tous les états** (52) : chaque écran charge sous une silhouette, échoue avec un bouton « Réessayer », et dit à quoi il sert quand il est vide ; un bandeau annonce le hors-ligne. Un test refuse un état écrit à la main.
      6. ✅ **Mouvement** (53) : une page qui glisse en s'ouvrant, la carte suivante qui entre, des confettis aux moments qui comptent, Rico qui réagit ; rien ne bouge pour qui a demandé moins d'animations (un filet global, et un test).
      7. ✅ **Accessibilité et vitesse vérifiées** (54, 55) : axe-core sur les pages d'un visiteur et d'un utilisateur connecté dans les deux thèmes, aucun défilement latéral à 320 px, un lien « Aller au contenu », un titre d'onglet par page ; la vitesse mesurée en CI sur un téléphone lent simulé. Ce qu'une machine ne voit pas est dans `docs/accessibilite.md`.
      8. ✅ **Tests avec cinq utilisateurs** (56) : le protocole, les neuf tâches, la grille d'observation et le questionnaire SUS sont dans `docs/tests-utilisateurs.md` ; les séances se mènent pendant la bêta.

   10. **CI plus rapide** (58 à 62) : un run prend 13 à 17 minutes. Deux PR :
       1. ✅ le job `docker` en parallèle des autres, et seuls les jobs concernés par les fichiers modifiés (un job `changes` les désigne) ;
       2. les jobs `e2e` et `docker` accélérés par leurs caches ; `content` et `runner` découpés par sujet quand le contenu C++ les aura trop allongés.

**Ordre** : l'étape 8.10.1 d'abord, puisqu'elle raccourcit toutes les PR suivantes ; puis les étapes 8.1 à 8.7, puis la refonte (8.9), puis le contenu (7, 4.2, 4.3 et 6), puis l'étape 8.8. Les règles de l'étape 8.2 s'appliquent à tout contenu écrit ensuite. La refonte passe avant l'étape 8.8 : ses nouveaux écrans naissent ainsi dans le bon style.

Le **déploiement** (phase 1, étape 7) vient après ces phases, avec les logs JSON, Sentry, HSTS, les sauvegardes de la base et les core dumps désactivés sur le serveur du runner (33, 46). Le reste de l'audit attend les retours de la bêta.

### Phase 4 : offre entreprise
Multi-organisation (colonne `organization_id` + Row-Level Security PostgreSQL), SSO entreprise via le fournisseur OIDC, tableau de bord admin avec statistiques **agrégées**, questions privées par organisation, export, conformité RGPD (registre, suppression de compte, DPA).

### Phase 5 : options
Correction des réponses ouvertes par IA, paiement. L'exécution de code en bac à sable et la rédaction assistée par IA sont avancées en phase 3 bis (ADR 0025).

---

## 10. Décisions encore ouvertes

À trancher au moment indiqué, avec un ADR :
1. ~~Contract-first OpenAPI ou génération de la spec depuis le code (phase 0).~~ **Tranché** : code-first (ADR 0003).
2. ~~NullAway/Error Prone activés dès le départ ou plus tard (phase 0).~~ **Tranché** : dès la phase 0 (ADR 0004).
3. ~~Type des identifiants : UUID v7 ou séquences (phase 1, avant la première migration métier).~~ **Tranché** : UUID v7 (ADR 0008).
4. ~~FSRS implémenté soi-même ou bibliothèque existante (phase 1).~~ **Tranché** : FSRS-6 implémenté dans le domaine (ADR 0011).
5. Fournisseur OIDC de production : managé (Zitadel Cloud, Clerk, Auth0...) ou Keycloak hébergé (phase 1, avant le déploiement).
6. Hébergeur (phase 1).
7. Nom du projet. **Provisoire** : Recallio (ADR 0005), à confirmer avant le premier déploiement public. Il ne doit pas évoquer Java seul.
8. ~~Format du manifeste des sujets (phase 1 bis, étape 1).~~ **Tranché** : pas de manifeste, un en-tête dans chaque document (ADR 0013).
9. ~~Répartition du quota de nouvelles cartes entre sujets suivis : parts égales, ou pondération choisie par l'utilisateur (phase 1 bis, étape 3).~~ **Tranché** : parts égales, en alternant les sujets (ADR 0014) ; revu par l'ADR 0048 : un quota par sujet, et une session par sujet.
10. ~~Bac à sable d'exécution du code des joueurs : service existant (Judge0, Piston) ou exécuteur maison à conteneurs jetables (phase 3 bis, étape 4).~~ **Tranché** : exécuteur maison à conteneurs jetables, un service `runner` séparé (ADR 0030).
11. ~~Rédaction du contenu à l'échelle.~~ **Tranché** : brouillons rédigés par Claude, chaque PR de contenu relue par le propriétaire avant le merge (ADR 0025).
12. ~~Le langage du Tour « Programmer de zéro » (phase 3 quater, étape 9).~~ **Tranché** : Java 25, ses fichiers source compacts étant faits pour débuter (ADR 0039).

---

## 11. Première session

Pour notre première session en mode plan :
1. Lis ce fichier et parcours rapidement la structure des documents de contenu (titres de chapitres, format des questions).
2. Pose-moi les questions nécessaires sur les points flous.
3. Propose le plan détaillé de la **phase 0**, découpé en étapes, avec pour chaque étape : objectif, fichiers concernés, notions à comprendre, critères de fin.
4. Attends ma validation avant de détailler la première étape.
