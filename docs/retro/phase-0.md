# Rétrospective : phase 0 (fondations)

- Période : du 2026-10-16 au 2026-10-29
- Livré : PR #1 à #10 (ADR, backend Quarkus, qualité, Dev Services, OpenAPI, frontend, CI, Docker)

## Critères de fin (CLAUDE.md, section 9)

| Critère | État |
|---|---|
| `./mvnw quarkus:dev` et `npm run dev` lancent tout en local | ✅ PostgreSQL et Keycloak démarrent seuls (Dev Services) |
| Le front appelle `/api/v1/health` | ✅ la page d'accueil affiche « API : UP » |
| La CI est verte | ✅ 3 contrôles requis sur `main` : backend, frontend, docker |
| ADR 0001 et 0002 | ✅ et 0003 à 0008 en plus |

## Ce qui a bien marché

- **Les garde-fous ont prouvé leur utilité dès la phase 0** :
  - le scan Trivy a bloqué une vulnérabilité critique (brotli) dans l'image nginx ;
  - le contrôle SHA-256 du wrapper Maven a refusé une distribution qui ne correspondait pas ;
  - le test de la spec OpenAPI sur un autre port a révélé des `servers` dépendant de l'environnement.
- **Les Dev Services** : aucune configuration de base ni de Keycloak à écrire pour développer.
- **Les ADR** : le passage par Java 21 puis le retour à Java 25 est tracé et compréhensible (ADR 0006 et 0007).

## Ce qui a coincé

- **Les PR empilées** : #5 à #7 ont été mergées dans leur branche de base au lieu de `main`, puis
  l'étape 6 a été commitée sur `main` en local. Il a fallu reconstruire l'historique (PR #8 et #9).
- **L'environnement Windows et PowerShell 5.1** :
  - les listes séparées par des virgules sont transformées en tableaux ;
  - les guillemets sont supprimés dans les arguments `--jq` ;
  - il n'y a pas de `&&`.
- **Des versions trop récentes pour l'écosystème** :
  - TypeScript 7, proposé par le modèle Vite, n'est supporté ni par `openapi-typescript` ni par `typescript-eslint` ;
  - Node 23 (non LTS) n'est pas supporté par jsdom et ESLint ;
  - npm 10.9 plante sur la résolution des dépendances de Vitest 5.
- **Un ADR écrit sans vérifier l'environnement** : l'ADR 0006 (Java 21) reposait sur une hypothèse
  fausse, puisque Java 25 était déjà installé.

## Ce qu'on change pour la phase 1

1. **Une seule branche à la fois**, créée depuis `main` à jour ; on merge avant de commencer l'étape suivante.
2. **Vérifier l'environnement** (`java -version`, `node -v`, ports occupés) avant toute décision qui en dépend.
3. **Commandes PowerShell** : une commande par ligne, valeurs entre guillemets simples, et Git Bash
   pour les filtres jq complexes.
4. **Node 24 LTS** en local, pour être aligné sur la CI.
5. Préférer la **dernière version stable largement supportée** plutôt que la toute dernière version
   majeure, sauf besoin réel (et alors, un ADR).
