# 0003. Spécification OpenAPI en code-first

- Statut : accepté
- Date : 2026-10-20

## Contexte

Le frontend doit utiliser un client API **généré** depuis une spécification
OpenAPI, jamais écrit à la main. Il faut décider qui fait foi : le fichier de
spécification ou le code Java.

Le cahier des charges (CLAUDE.md, section 4.4) privilégiait le contract-first, tout en
laissant le code-first comme alternative à trancher en phase 0.

## Options envisagées

1. **Contract-first** : on écrit `openapi.yaml`, puis on génère les interfaces serveur et le client TS
   - ✅ Le contrat est pensé avant l'implémentation ; excellent pour le design d'API
   - ❌ Outillage supplémentaire (openapi-generator côté serveur), code généré moins lisible
   - ❌ Double travail au quotidien : modifier le YAML, régénérer, puis implémenter
2. **Code-first** : les ressources JAX-RS annotées produisent la spec via smallrye-openapi
   - ✅ Une seule source de vérité : le code, qui compile et est testé
   - ✅ Intégré nativement à Quarkus, aucune génération côté serveur
   - ❌ Le contrat est un produit dérivé : une modification d'API peut passer inaperçue

## Décision

On choisit le **code-first** (option 2).

- La spec est générée par `quarkus-smallrye-openapi` au build et **exportée dans le
  dépôt** (`backend/src/main/openapi/` ou équivalent), où elle est versionnée.
- Le client TypeScript du frontend est généré à partir de ce fichier.
- Les endpoints sont documentés avec les annotations MicroProfile OpenAPI
  (`@Operation`, `@APIResponse`…) pour obtenir une spec de qualité.

## Conséquences

- ➕ Moins de friction : une évolution d'API se fait en un seul endroit.
- ➕ Le diff de la spec dans chaque PR rend visible tout changement de contrat.
- ➖ La CI doit vérifier que la spec committée est à jour (build, puis `git diff --exit-code`).
- ➖ Il faut de la rigueur sur les annotations, sinon la spec reste pauvre.
- 🔁 À reconsidérer si un consommateur externe de l'API apparaît (le contrat devrait
  alors être conçu et stabilisé avant le code).
