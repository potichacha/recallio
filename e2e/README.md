# Parcours de bout en bout (Playwright)

Quelques parcours critiques joués dans un vrai navigateur, contre la pile complète « comme en production » : PWA
servie par nginx, backend, PostgreSQL et Keycloak (CLAUDE.md, section 8). Ils complètent les tests unitaires et
d'intégration, sans les remplacer : un parcours par fonctionnalité clé, pas un test par écran.

| Parcours                                                                           | Fichier                        |
| ---------------------------------------------------------------------------------- | ------------------------------ |
| Se connecter par Keycloak, revenir sur la page demandée, se déconnecter            | `tests/sign-in.spec.ts`        |
| Réviser une session (aussi sur mobile), réviser un seul thème, voir sa progression | `tests/review-session.spec.ts` |
| Atteindre l'objectif du jour et démarrer son streak                                | `tests/streak.spec.ts`         |

## Lancer en local

Prérequis : Docker, Node.js (voir `frontend/.nvmrc`). Les ports 3000 et 8180 doivent être libres (arrêter
`quarkus:dev` et le compose habituel).

```powershell
# 1. La pile de test : base neuve (projet « recallio-e2e »), objectif quotidien de 3 cartes
docker compose -p recallio-e2e -f infra/compose.yaml -f infra/compose.e2e.yaml up --build --wait

# 2. Les parcours
cd e2e
npm ci
npx playwright install chromium   # une seule fois
npm test                          # ou npm run test:ui pour les suivre pas à pas

# 3. Nettoyer (supprime aussi la base de test)
docker compose -p recallio-e2e -f infra/compose.yaml -f infra/compose.e2e.yaml down -v
```

En cas d'échec, `test-results/` contient une capture d'écran et une trace (`npx playwright show-trace <fichier>`).

## Règles d'écriture

- Sélecteurs accessibles (`getByRole`, `getByLabel`), comme un utilisateur ou un lecteur d'écran ; pas de classes CSS.
- Pas d'attente fixe (`waitForTimeout`) : les assertions `expect` attendent d'elles-mêmes.
- Un utilisateur par parcours (alice, bob) et des assertions qui tolèrent une base déjà utilisée : relancer les tests
  sans nettoyer reste possible.
