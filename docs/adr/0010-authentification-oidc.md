# 0010. Authentification OIDC de la PWA et profil utilisateur

- Statut : accepté
- Date : 2026-10-29

## Contexte

La révision, la progression et la gamification sont personnelles : il faut identifier l'utilisateur. Le backend
doit rester agnostique du fournisseur (ADR 0002) : Keycloak en local, un fournisseur managé ou un Keycloak hébergé
en production (décision encore ouverte). La PWA est une application publique, sans secret possible côté client.

## Options envisagées

1. **Sessions gérées par le backend** (formulaire de login maison, cookie de session)
   - ✅ Simple à comprendre
   - ❌ Gérer les mots de passe, la réinitialisation et la sécurité soi-même ; contraire à la stack décidée
2. **Backend-for-Frontend** : le backend fait le flux OIDC et pose un cookie HttpOnly
   - ✅ Aucun jeton accessible au JavaScript
   - ❌ Le backend devient étatful (sessions) et doit gérer le CSRF ; plus complexe pour une PWA hors ligne
3. **PWA cliente OIDC publique** (Authorization Code + PKCE), le backend valide des JWT bearer
   - ✅ Standard, sans secret côté client ; le backend reste sans état
   - ✅ Changer de fournisseur ne change que la configuration
   - ❌ Le jeton d'accès est manipulé par le JavaScript : il faut limiter sa durée de vie et le risque XSS

## Décision

On choisit l'option 3.

- **PWA** : `oidc-client-ts`, via `react-oidc-context`, avec le flux **Authorization Code + PKCE**. Le client
  `recallio-frontend` est public. Les jetons sont gardés en **sessionStorage** (rien de durable), et renouvelés
  automatiquement. Les composants n'utilisent qu'une interface `Session` qui leur est propre : la bibliothèque est
  confinée à `OidcSessionProvider`.
- **Backend** : `quarkus-oidc` en mode `service`. Il vérifie la signature, l'émetteur, l'expiration et
  l'**audience** `recallio-api` (ajoutée par un mapper du royaume) : un jeton émis pour une autre application du même
  fournisseur est refusé.
- **Profil** : créé au premier appel de `GET /api/v1/me`, identifié par le claim `sub`. La création est atomique
  (`INSERT … ON CONFLICT DO NOTHING`) pour supporter deux premières connexions simultanées. Le nom vient du claim
  `name` (sinon `preferred_username`), le fuseau de l'en-tête `X-Timezone` envoyé par le navigateur, sinon `UTC`.
- **Rôles** : le rôle de royaume `admin` donne accès aux tâches d'administration (import du contenu).
- **Local** : Dev Services et le compose importent le même royaume (`infra/keycloak/recallio-realm.json`) avec deux
  utilisateurs, `alice` (admin) et `bob`. En dev, Keycloak écoute sur un port fixe (8180), pour que la PWA le trouve.

## Conséquences

- ➕ Aucun mot de passe ni e-mail stocké par Recallio : le fournisseur porte l'identité (et le RGPD en est simplifié).
- ➕ Le backend reste sans état et se teste avec `@TestSecurity` et `@OidcSecurity`, sans fournisseur réel.
- ➖ Un XSS permettrait de lire le jeton d'accès : on garde des jetons courts (5 minutes), aucun HTML interprété dans le
  contenu (react-markdown), et une politique CSP à ajouter avant la production.
- ➖ Le Keycloak des Dev Services et celui du compose utilisent tous deux le port 8180 : on n'en lance qu'un à la fois.
- 🔁 Si le risque XSS devient inacceptable (contenu saisi par les utilisateurs, phase 4), passer à l'option 2 (BFF).
