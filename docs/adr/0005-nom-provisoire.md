# 0005. Nom provisoire du projet : Recallio

- Statut : accepté (provisoire)
- Date : 2026-10-20

## Contexte

Le projet a besoin d'un nom dès la phase 0 : package Java racine, nom du dépôt
GitHub, README, futures images Docker. Le nom définitif fait partie des décisions
ouvertes du cahier des charges (CLAUDE.md, section 10).

Critères :
- court et prononçable en français comme en anglais ;
- neutre vis-à-vis du sujet (Java aujourd'hui ; C++, IA ou prompting demain) ;
- évocateur de la mémorisation ou de la préparation d'entretien ;
- utilisable comme package Java.

## Options envisagées

1. **Recallio** (recall + io) : évoque la mémorisation, sonne comme un produit
2. **Prepdeck** (préparation + deck de cartes) : très explicite, mais centré sur les cartes
3. **Ripeto** (« je répète » en italien) : allusion à la répétition espacée, moins transparent
4. **Dailyprep** : met en avant la révision quotidienne, mais reste générique

## Décision

On retient **Recallio** comme nom **provisoire** : dépôt `recallio`, package
racine `com.recallio`.

## Conséquences

- ➕ Le nom est utilisable immédiatement dans le code et l'infrastructure.
- ➖ La disponibilité du nom de domaine et l'absence d'homonyme gênant (marque
  déposée, produit existant) ne sont pas encore vérifiées.
- ➖ Un renommage est bon marché tant que le code est petit, mais il deviendra
  coûteux après le déploiement (URL, configuration OIDC, images).
- 🔁 Le nom doit être confirmé ou remplacé, par un nouvel ADR, **avant le premier
  déploiement public** (phase 1).
