# Les maquettes de la refonte « trois portes » (ADR 0077)

Les 35 planches de la refonte, telles qu'elles ont été dessinées le 2026-11-26. Elles font référence pour la structure,
les textes, les tailles, les espacements et les couleurs de chaque écran ; là où une planche contredit une règle du
design system, la règle gagne (ADR 0077).

## Lire une planche

Chaque fichier `.dc.html` est une planche : son balisage est entre `<x-dc>` et `</x-dc>`, avec ses styles en ligne.
`kit.css` porte les recettes communes (boutons, cartes, pastilles, navigation), les aplats de Rico et le mouvement.
`canvas.json` donne le titre et la taille de chaque planche.

Ces fichiers viennent d'un canevas de maquettes : ils se lisent comme du HTML, mais ne s'ouvrent pas tels quels dans un
navigateur (il leur manque le script du canevas).

## Les planches

| Porte | Planches |
|---|---|
| Apprendre | `AccueilV2`, `CheminV2`, `CheminLong`, `Lecon`, `LeconV2` (réponse juste), `VerdictFaux`, `FinLecon`, `LeconCode`, `Exercice`, `Revision`, `Amis` |
| S'entraîner | `Intensif`, `Question`, `Bilan`, `Plus`, `Code` |
| Jouer | `Partie`, `Rejoindre`, `Live`, `LiveVerdict`, `Classement`, `Partage` |
| Premier contact | `Landing`, `Onboarding`, `TestNiveau` |
| États | `Vide`, `Erreur`, `Chargement` |
| Thème sombre | `AccueilSombre`, `LeconSombre`, `RevisionSombre` |
| Ordinateur | `AccueilBureau`, `LeconBureau`, `HoteBureau` |
| Rico | `RicoPoses` (seize poses) |

## Ce qui n'a pas de planche

Le profil, les groupes, le classé, l'examen blanc, le défi du jour, le cours et les pages des admins : ils gardent leur
écran et passent au nouveau style avec les recettes de `kit.css`.

Les données sont des exemples : les noms, les scores, les 20 questions gratuites par jour, le prix.
