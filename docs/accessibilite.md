# Accessibilité : ce qui est vérifié, et ce qui reste à vérifier à la main

Recallio vise le niveau AA de WCAG 2.2, dans ses deux thèmes (ADR 0058). Une machine en vérifie une partie à chaque
pull request ; le reste demande quelqu'un, un clavier et un lecteur d'écran. Ce document dit lequel est lequel.

## Ce que la CI vérifie

| Contrôle | Où | Critères WCAG |
|---|---|---|
| Contrastes des tokens, dans les deux thèmes : 4,5 pour un texte, 3 pour un contour ou un pictogramme | `frontend/src/design/contrast.test.ts` | 1.4.3, 1.4.11 |
| axe-core sur les pages d'un visiteur et d'un utilisateur connecté, dans les deux thèmes, et sur une carte corrigée | `e2e/tests/accessibility.spec.ts` | ce qu'axe sait mesurer des niveaux A et AA |
| Aucun défilement latéral à 320 px de large (un écran de 1280 px zoomé à 400 %) | `e2e/tests/accessibility.spec.ts` | 1.4.10 |
| Le premier arrêt du clavier saute la navigation | `e2e/tests/accessibility.spec.ts` | 2.4.1 |
| Chaque page donne son titre à l'onglet | `e2e/tests/accessibility.spec.ts` | 2.4.2 |
| Une session se joue au clavier seul, et le focus passe sur « Carte suivante » | `e2e/tests/review-session.spec.ts` | 2.1.1, 2.4.3 |
| Aucune animation ne tourne pour qui a demandé moins d'animations | `e2e/tests/motion.spec.ts` | 2.3.3 |
| Un verdict a une icône et un texte, jamais la couleur seule | `frontend/src/components/ChoiceOption.test.tsx` | 1.4.1 |
| Aucun état de chargement ou d'erreur écrit à la main, aucune animation sans garde-fou | `frontend/src/design/designRules.test.ts` | règles du design system |

Seules les violations **graves ou critiques** d'axe font échouer la CI. Les mineures apparaissent dans le rapport
Playwright.

## Ce qu'aucune machine ne vérifie

axe trouve à peu près un tiers des défauts d'accessibilité. Le reste se vérifie à la main, avant la bêta puis à chaque
refonte d'un écran.

### Au lecteur d'écran

Avec NVDA (Windows, gratuit) ou VoiceOver (macOS, iOS), sur le parcours principal : ouvrir l'accueil, essayer sans
compte, se connecter, lancer une session, répondre à trois cartes, lire le bilan.

- [ ] Le titre de la page est annoncé à chaque changement de page.
- [ ] Les titres (`h1`, `h2`, `h3`) dessinent le plan de la page, sans niveau sauté.
- [ ] Chaque bouton et chaque lien dit ce qu'il fait hors de son contexte (« Essayer Java », pas « Essayer »).
- [ ] La question, ses choix et leur nombre sont lus dans l'ordre ; « Une seule réponse » ou « Plusieurs réponses
      possibles » est annoncé.
- [ ] Après « Valider », le verdict est annoncé sans qu'on ait à le chercher, puis l'explication se lit.
- [ ] Un bloc de code est lu comme du code, sans que la coloration ajoute du bruit.
- [ ] Rico n'est jamais annoncé quand le texte voisin dit déjà tout.
- [ ] Le bandeau « hors ligne » et l'objectif atteint sont annoncés quand ils apparaissent.

### Au clavier seul

- [ ] Tout s'atteint avec Tab, dans l'ordre de la lecture ; rien ne retient le focus.
- [ ] Le focus se voit toujours, dans les deux thèmes, y compris sur la carte « Aujourd'hui ».
- [ ] L'éditeur de code se quitte au clavier (Échap, puis Tab).
- [ ] Une partie live et un duel se jouent sans souris.

### À l'œil

- [ ] Zoom du navigateur à 200 % sur un ordinateur : rien n'est coupé, rien ne se chevauche.
- [ ] Taille du texte du téléphone au maximum : les boutons restent lisibles.
- [ ] Les deux thèmes, écran par écran : aucun texte gris sur gris, aucune icône qui disparaît.
- [ ] Une cible tactile fait au moins 44 px (les liens d'une phrase sont l'exception admise).

## Quand on trouve un défaut

Un défaut d'accessibilité est un bug : il reçoit un test qui le reproduit quand une machine peut le voir (CLAUDE.md,
section 8), et une ligne dans ce document quand elle ne le peut pas.
