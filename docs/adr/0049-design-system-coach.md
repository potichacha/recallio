# 0049. Identité visuelle : le design system « Coach »

- Statut : accepté
- Date : 2026-11-05

## Contexte

En testant l'application avant le déploiement, le propriétaire a demandé un frontend qui n'ait pas « l'air fait par
Claude ». Il voulait aussi qu'il suive les principes d'IHM et les pratiques qui retiennent les utilisateurs.

Les guides sur l'apparence « générée par IA » s'accordent : elle vient d'une accumulation de choix par défaut, pas d'un
détail. Ils citent :
- les dégradés violets ;
- la police par défaut ;
- les mêmes cartes arrondies avec ombre partout ;
- les émojis en guise d'icônes ;
- les petits libellés en majuscules au-dessus des titres.

Le code en était plein, d'après ce qui a été mesuré :

- **La palette par défaut de Tailwind** : 689 classes de couleur dans 68 fichiers, dont 218 indigo, et un dégradé
  indigo-violet sur les cartes de partage.
- **Une seule carte** : la même carte blanche arrondie, avec bordure et ombre, dans 36 fichiers.
- **La police système**, et une hiérarchie plate.
- **31 émojis en guise d'icônes** (🔥 ❄️ 🏆 🥇…).

Trois pistes ont été dessinées sur les mêmes écrans (accueil, carte retournée, fin de session) :
- **A · Fiche bristol** : la fiche de révision en papier ;
- **B · Atelier de code** : un éditeur sombre et sobre ;
- **C · Coach** : une appli d'entraînement.

Le propriétaire a retenu la piste C.

## Options envisagées

- **La piste A** : la plus singulière, mais studieuse, moins motivante au quotidien.
- **La piste B** : elle parle aux développeurs, mais son style est courant, et froid pour « Programmer de zéro ».
- **La piste C** (retenue) : elle reprend les codes des applis d'habitude (série, anneau d'objectif, boutons épais)
  sans copier le vert de Duolingo. C'est la plus engageante pour une révision quotidienne.

Pour les polices : le CDN de Google Fonts, ou des fichiers servis par l'application (retenu). Charger les polices
depuis Google transmet l'adresse IP des visiteurs ; un tribunal allemand l'a jugé contraire au RGPD en 2022. Des
fichiers servis par l'application sont mis en cache par la PWA et fonctionnent hors ligne.

Pour les icônes :
- **des SVG écrits à la main** : aucune dépendance, mais tout à dessiner et à maintenir ;
- **Lucide** : le jeu par défaut de shadcn, omniprésent dans les interfaces générées ;
- **Phosphor** (retenu, MIT) : ses graisses « bold » et « fill » vont avec les boutons épais du Coach.

## Décision

1. **La piste « Coach »**.
   - **Couleurs** : un sarcelle `#0E6B63` pour les actions, un jaune « soleil » `#FFC83D` pour l'XP et les séries, et
     une flamme orange pour la série. Le fond est gris très clair, les cartes blanches.
   - **Formes** : des cartes arrondies (20 px), avec un bord épais dessous au lieu d'une ombre floue. Des boutons
     arrondis (14 px), le principal avec un bord épais qui s'enfonce au clic.
2. **Seulement des tokens sémantiques**, dans `frontend/src/index.css` (`@theme`).
   - **Familles** : `ink`, `surface`, `canvas`, `line`, `brand`, `sun`, `success`, `danger`, `warning`, `info` ;
     `code` pour le code ; `tier-*` pour les rangs du classé.
   - **Retrait de la palette par défaut** de Tailwind (`--color-*: initial`) : `bg-indigo-600` ne produit plus rien.
3. **Le mode sombre** suit le réglage du système (`prefers-color-scheme`).
   - Les rôles sont les mêmes, avec d'autres valeurs.
   - Les contrastes des paires texte/fond principales sont au niveau AA (4,5:1).
   - Le code reste sombre dans les deux thèmes : blocs, sorties de programme et éditeur, dont la coloration
     syntaxique prend les mêmes tokens.
4. **Les polices**.
   - **Lexend** pour le texte : elle est dessinée pour la lisibilité.
   - **JetBrains Mono** pour le code.
   - Toutes deux variables et servies par l'application (`@fontsource-variable/*`, OFL), puis précachées par la PWA.
5. **Les icônes Phosphor, jamais d'émoji**.
   - Une icône est décorative (`aria-hidden`), et le texte porte le sens.
   - Quand une icône était le seul signal (un test réussi ou échoué), un texte pour lecteur d'écran l'accompagne.
6. **Une seule recette de bouton** : `button({ variant, size })` (`components/ui/button.ts`), pour un `<button>` comme
   pour un `<Link>`.
7. **Des garde-fous** : `src/design/designRules.test.ts` échoue, à chaque `npm test`, sur une classe de la palette par
   défaut, un dégradé ou un émoji dans le code de l'interface.
8. **Le tutoiement partout** : l'application « tutoie » déjà dans le Tour (« À toi »). Il sera généralisé à l'étape des
   autres écrans.
9. **La migration**.
   - Les couleurs et les boutons sont migrés par un script, de façon mécanique.
   - La structure des écrans change dans les étapes suivantes de la refonte : navigation, accueil, révision, Tour…

## Conséquences

- ➕ Une identité propre et cohérente, en clair comme en sombre. Aucune couleur hors des tokens, et le test le garantit.
- ➕ Le mode sombre ne demande aucun travail écran par écran : un composant qui emploie les tokens s'adapte seul.
- ➖ Plus de poids : 21 Ko de JavaScript compressé pour 25 icônes, chacune avec ses six graisses. Les polices latines
  pèsent environ 80 Ko, précachées.
- ➖ Deux dépendances déclarées pour la coloration de l'éditeur (`@codemirror/language`, `@lezer/highlight`). Elles
  étaient déjà installées indirectement.
- ➖ Les couleurs de la carte de partage sont recopiées dans `shareCard.ts`, car une image dessinée ne lit pas les
  variables CSS.
- 🔁 D'ici les étapes suivantes, les écrans gardent leur structure : navigation en haut, petits libellés en majuscules.
