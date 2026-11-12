# 0058. La refonte « Rico » : une identité, deux thèmes

- Statut : accepté
- Date : 2026-11-12
- Révise : ADR 0049 (le design system « Coach ») sur les couleurs, la police et le thème sombre
- Prolonge : ADR 0050 (le parcours), ADR 0054 (Content-Security-Policy), ADR 0057 (pas d'annulation d'une réponse)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), éléments 47 à 57

## Contexte

Le design system « Coach » (ADR 0049) a donné à l'application une interface propre et cohérente. Le propriétaire veut
davantage : une identité qu'on reconnaît, « comme les sites à 10 000 € », sans budget. Parmi trois directions maquettées
le 2026-11-10 (ludique, éditoriale, atelier sombre), il a retenu la **ludique** : une mascotte, Rico ; du violet et du
jaune ; des formes rondes et des boutons épais. Il l'a voulue en clair et en sombre, au choix de l'utilisateur.

Les maquettes ont été auditées avant le choix (contrastes selon WCAG 2.2 AA, ergonomie selon l'ISO 9241-110). Leurs
défauts font partie de ce que la refonte corrige.

## Options envisagées

**La portée du changement** :

- **Un nouveau design system**, écrit à côté de l'ancien : deux jeux de tokens à faire vivre le temps de la migration.
- **Les mêmes tokens, d'autres valeurs** (retenu) : l'ADR 0049 a nommé les couleurs par leur rôle. Changer d'identité,
  c'est donc changer leurs valeurs, et aucun écran n'est à réécrire pour cela.

**Le thème sombre** :

- **Suivre le système seulement**, comme l'ADR 0049 : pas de réglage, mais pas de choix non plus.
- **Un choix gardé sur le serveur**, dans le profil : une migration et une route, pour un réglage d'affichage qui dépend
  de l'appareil (on lit en sombre sur son téléphone le soir, pas forcément au bureau).
- **Un choix gardé sur l'appareil, le système par défaut** (retenu).

**Comment éviter l'éclair de thème clair à l'ouverture** :

- **Un script dans la page** : la Content-Security-Policy n'en admet aucun (ADR 0054).
- **Appliquer le thème au démarrage de React** : trop tard, la page est déjà affichée.
- **Un petit fichier chargé dans l'en-tête** (retenu) : `public/theme.js`, servi par l'application.

**Rico** :

- **Des images** (PNG ou SVG en fichier) : elles ne suivent pas le thème.
- **Un composant SVG dessiné avec les tokens** (retenu).

## Décision

**Ce qui change par rapport à l'ADR 0049** :

| | « Coach » (ADR 0049) | « Rico » |
|---|---|---|
| Couleur de marque | sarcelle `#0e6b63` | violet `#5b3df5` (clair), `#a99bff` (sombre) |
| Accent | jaune `#ffc83d` | jaune `#ffc531` |
| Police | Lexend | Nunito, servie par l'application |
| Thème sombre | celui du système | celui du système, ou clair ou sombre au choix dans le profil |
| Boutons | bord en relief | plus épais et plus ronds, 44 px de haut au moins |

**Ce qui ne change pas** : des tokens nommés par leur rôle, la palette par défaut de Tailwind retirée, ni dégradé ni
émoji, les icônes Phosphor, JetBrains Mono pour le code, `button()` pour tous les boutons, le tutoiement, et le test
qui garde ces règles (`designRules.test.ts`).

**Le thème** : la page porte `data-theme="light"` ou `"dark"`, que lisent les tokens de `index.css`. `public/theme.js`
le pose avant le premier affichage, d'après le choix gardé sur l'appareil, ou sinon le système ; `src/theme/theme.ts`
applique la même règle quand l'utilisateur choisit, dans la section Réglages du profil.

**Les règles d'accessibilité de la direction**, issues de l'audit des maquettes :

- un texte a au moins 4,5 de contraste avec son fond, le contour d'un contrôle ou un pictogramme au moins 3, dans les
  deux thèmes ; `contrast.test.ts` le calcule sur les tokens de `index.css` à chaque `npm test` ;
- un texte blanc ne se pose que sur le vert foncé, jamais sur un vert vif ;
- aucune information ne repose sur la seule couleur : un lien dans une phrase est souligné, un choix sélectionné a sa
  pastille, une réponse juste son icône ;
- toucher un choix le sélectionne, « Valider » l'envoie ;
- le focus se voit, une cible fait 44 px, les animations respectent « réduire les animations ».

La maquette prévoyait aussi « Annuler ma réponse » après le verdict : ce point est **retiré**, l'ADR 0057 ayant renoncé
à l'annulation.

**Rico** : un composant (`Rico.tsx`), trois humeurs (content, encourageant, pensif). C'est un dessin de principe : un
illustrateur fera mieux, et le remplacer ne touche que ce fichier.

**Le déroulé**, une pull request par étape (CLAUDE.md, étape 8.9) : l'identité (cette étape), la page publique,
l'accueil connecté, la carte de révision, les états de chaque écran, le mouvement, l'accessibilité et la vitesse
vérifiées, puis le protocole des tests avec cinq utilisateurs.

## Conséquences

- L'application entière change de couleurs et de police en une étape, parce qu'aucun écran ne nommait une teinte.
- Le thème sombre ne s'applique plus sans JavaScript : sans lui, la page reste claire. L'application ne fonctionne de
  toute façon pas sans JavaScript.
- La règle du thème est écrite deux fois, dans `public/theme.js` et dans `src/theme/theme.ts` : dix lignes, qu'un test
  compare en exécutant le script de la page.
- Les contrastes sont gardés par un test : choisir une couleur plus jolie mais illisible fait échouer la CI. Les
  parcours axe-core passent aussi dans le thème sombre.
- Les cartes de partage (ADR 0045) et les icônes de la PWA portent des couleurs en dur : elles ont été refaites, et le
  seront encore si la marque change (`e2e/scripts/generate-pwa-icons.mjs`).
- Ce qui demande un budget reste hors de la feuille de route : un illustrateur pour Rico, un motion designer, une
  vidéo, des applications natives.
