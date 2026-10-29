# 0002. Stack technique

- Statut : accepté
- Date : 2026-10-20

## Contexte

Recallio est une application web utilisable sur mobile (PWA), développée par un
développeur backend Java seul. Le projet a deux objectifs : progresser
techniquement et servir de projet de portfolio.

Contraintes :
- Le code doit rester maintenable sur plusieurs années.
- L'hébergement doit être européen et bon marché.
- Le lancement en local doit prendre moins de cinq minutes.
- Le fournisseur d'identité de production n'est pas encore choisi.

## Options envisagées

1. **Spring Boot + JPA + React**
   - ✅ Écosystème le plus répandu, très demandé en entretien
   - ❌ Déjà bien connu : moins de progression ; démarrage et mémoire plus lourds
2. **Quarkus + Hibernate ORM + PostgreSQL + React**
   - ✅ Dev Services (PostgreSQL, Keycloak lancés automatiquement), live reload, démarrage rapide
   - ✅ Standards Jakarta EE / MicroProfile : connaissances transférables
   - ❌ Communauté plus petite que Spring, moins de réponses toutes faites
3. **Full-stack JavaScript (Node + Next.js)**
   - ✅ Un seul langage
   - ❌ Hors du cœur de compétence visé (backend Java)

## Décision

On choisit l'option 2 :

| Domaine | Choix | Justification |
|---|---|---|
| Langage | Java 25 (LTS) | `record`, `sealed`, pattern matching ; LTS donc support long |
| Framework | Quarkus 3.x | Dev Services, live reload, standards MicroProfile |
| Build | Maven + wrapper `mvnw` | build reproductible sans installation préalable de Maven |
| Base de données | PostgreSQL + Flyway | base relationnelle robuste ; schéma versionné, jamais généré hors tests |
| Authentification | OIDC standard (`quarkus-oidc`) | backend agnostique : changer de fournisseur ne touche pas le code ; Keycloak en local |
| Frontend | React + TypeScript + Vite, PWA | écosystème mature, typage, installable sur mobile sans application native |
| Appels API front | TanStack Query + client généré | cache et états de requête gérés ; client toujours aligné sur l'API |
| Tests backend | JUnit, AssertJ, Mockito, `@QuarkusTest`, REST Assured, Testcontainers, ArchUnit | du test unitaire du domaine aux tests d'intégration sur un vrai PostgreSQL |
| Tests frontend | Vitest, Testing Library, Playwright | intégré à Vite ; quelques parcours de bout en bout |
| Qualité | Spotless, Error Prone + NullAway (voir ADR 0004), ESLint + Prettier | formatage et analyse automatiques, pas de débat de style en revue |
| CI | GitHub Actions | intégrée au dépôt, gratuite pour un dépôt public |
| Conteneurs | Docker, `docker compose` | même image du local à la production |

Restent ouverts et feront l'objet d'ADR dédiés : l'hébergeur, le fournisseur OIDC
de production, le type des identifiants et l'implémentation de FSRS (phase 1).
Valkey/Redis et WebSockets ne seront introduits qu'aux phases 2 et 3.

## Conséquences

- ➕ Une seule commande (`./mvnw quarkus:dev`) lance le backend avec sa base et son
  fournisseur d'identité.
- ➕ Le client TypeScript généré supprime toute une classe de bugs d'intégration.
- ➖ Docker est obligatoire en local et en CI (Dev Services, Testcontainers).
- ➖ Java 25 est récent : certains outils (Error Prone, plugins Maven) peuvent
  nécessiter des versions récentes. À vérifier à chaque montée de version.
- 🔁 Toute nouvelle dépendance structurante passe par un ADR.
