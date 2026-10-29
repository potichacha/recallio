# Rétrospective : phase 1 (MVP révision solo)

- Période : 2026-10-29
- Livré : PR #17 à #21, et celle des parcours de bout en bout (import du contenu, OIDC, révision FSRS, gamification,
  tableau de bord)

## Critères de fin (CLAUDE.md, section 9)

| Étape ou critère | État |
|---|---|
| 1. Parseur et import du contenu, consultation | ✅ 1 311 cartes dans 43 thèmes, sans anomalie ; import idempotent (ADR 0009) |
| 2. Authentification OIDC, profil au premier login | ✅ PKCE, audience vérifiée, profil créé avec le fuseau du navigateur (ADR 0010) |
| 3. Révision avec auto-évaluation et FSRS | ✅ FSRS-6 vérifié contre py-fsrs, réponses idempotentes (ADR 0011) |
| 4. Points, objectif quotidien et streak | ✅ registre append-only, streak dans le fuseau de l'utilisateur (ADR 0012) |
| 5. Tableau de bord personnel | ✅ cartes dues, série, 7 derniers jours, progression par thème |
| 6. Déploiement et observabilité de base | ⏸️ **en attente** : hébergeur et fournisseur OIDC de production à choisir (CLAUDE.md, section 10) |
| Des testeurs révisent chaque jour sur mobile ; rétention à 7 jours | ⏸️ dépend du déploiement |
| Parcours de bout en bout (CLAUDE.md, section 8) | ✅ 6 parcours Playwright, dont un sur écran mobile, dans la CI |

La phase 1 est **fonctionnellement terminée** ; elle ne sera close qu'après le déploiement et une première mesure de
rétention.

## Ce qui a bien marché

- **Tester contre une référence externe.** Les scénarios de py-fsrs ont validé l'implémentation de FSRS-6 du premier
  coup : sans eux, une erreur de formule serait passée inaperçue (les intervalles « ont l'air » plausibles).
- **Les tests d'intégration sur un vrai PostgreSQL ont trouvé de vrais bugs**, invisibles avec des faux :
  - Hibernate lit les valeurs d'une nouvelle entité au moment du `persist` (entité remplie trop tard) ;
  - l'horloge Windows est plus précise que `timestamptz` : une réponse rejouée ne renvoyait pas le même instant.
- **Les frontières de modules ont tenu** : façades (`CardDirectory`, `CurrentUser`), événement `CardReviewed`, aucune
  clé étrangère entre modules. ArchUnit est resté vert sans exception à consigner.
- **Les vérifications en conditions réelles** (vraie connexion Keycloak, vrai catalogue) en plus des tests : elles ont
  confirmé le comportement de bout en bout avant chaque PR.

## Ce qui a coincé

- **`localhost` en IPv6** : trois fois dans la phase. Un script Python mettait 2 s par appel, et les sondes de santé
  nginx et backend échouaient dans les conteneurs. À chaque fois, `127.0.0.1` a réglé le problème.
- **Node 23 en local** (point 4 de la rétro de phase 0, non fait) : les tests du frontend ont dû tourner dans un
  conteneur `node:24-alpine`.
- **Une course dans un parcours Playwright** : React Router met à jour l'URL avant d'afficher la page. Attendre l'URL
  ne suffit pas, il faut attendre un élément de la nouvelle page.
- **Des réglages globaux** (quota de nouvelles cartes, objectif quotidien) alors qu'ils relèvent du choix de chaque
  utilisateur : acceptable pour un MVP, à revoir avec les groupes.

## Ce qu'on change pour la suite

1. **Installer Node 24** en local (`fnm install 24`), cette fois pour de bon.
2. **Rendre le contrôle `e2e` obligatoire** sur `main`, comme `backend`, `frontend` et `docker`.
3. **`127.0.0.1` plutôt que `localhost`** dans les sondes, scripts et URL internes aux conteneurs.
4. **Trancher l'hébergeur et le fournisseur OIDC** (ADR) pour déployer avant d'aller loin dans la phase 2 : les retours
   de vrais utilisateurs valent plus que de nouvelles fonctionnalités.
5. Dans les parcours Playwright, **attendre un élément de la page cible** après chaque navigation.
