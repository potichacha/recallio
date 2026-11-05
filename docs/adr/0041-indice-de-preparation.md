# 0041. L'indice de préparation

- Statut : accepté
- Date : 2026-11-05

## Contexte

Quelqu'un qui prépare un entretien veut savoir s'il est prêt, comme l'élève d'Ornikar qui voit s'il est « prêt pour
l'examen » (CLAUDE.md, phase 3 ter, étape 7). Le tableau de bord donne déjà la maîtrise des notions de chaque sujet
(ADR 0034), mais toutes les notions y pèsent autant, de l'essentiel au rare : on peut maîtriser cent notions rares et
rater la question sur `equals` et `hashCode`.

Le référentiel classe chaque notion par priorité (1 essentiel, 2 courant, 3 avancé, 4 rare, ADR 0025). Les notions
essentielles sont celles qu'un entretien demande presque toujours.

## Options envisagées

- **La part des notions acquises, toutes priorités** : elle mesure l'avancement, pas la préparation, et un gros sujet
  n'est jamais « prêt ».
- **Une note pondérée par priorité** (1 vaut 4, 2 vaut 3…) : difficile à expliquer, et l'essentiel peut rester troué.
- **La part des notions essentielles acquises, puis maîtrisées** (retenu) : simple à dire, et elle vise ce que
  l'entretien demande.

## Décision

- **L'indice** d'un sujet ne regarde que ses notions de priorité 1. Une notion acquise compte pour un demi-point, une
  notion maîtrisée ou experte pour un point ; l'indice est le total rapporté au nombre de notions essentielles, en
  pourcentage arrondi à l'inférieur. On n'atteint 100 % qu'en maîtrisant toutes les notions essentielles.
- **Prêt** à partir de 80 % : par exemple, trois cinquièmes des notions essentielles maîtrisées et le reste acquis.
- **Le calcul** est fait dans le domaine du module `review` (`Readiness`), à partir de la maîtrise des notions déjà
  calculée pour le tableau de bord : aucune requête de plus, rien de stocké.
- **L'API** : chaque sujet des statistiques de révision (`GET /api/v1/me/review-stats`) porte son indice (notions
  essentielles, acquises, maîtrisées, pourcentage et « prêt »). Le seuil est appliqué par le serveur.
- **La PWA** : une jauge « Prêt pour l'entretien » sous la maîtrise des notions de chaque sujet du tableau de bord.

## Conséquences

- Un sujet sans référentiel (un document Markdown) n'a pas d'indice, et la jauge ne s'affiche pas.
- L'indice suit la mémoire : une notion maîtrisée qu'on oublie (stabilité qui retombe) le fait baisser. Il dit si l'on
  est prêt aujourd'hui, pas si l'on l'a été.
- L'examen blanc (étape 8) mesurera la même chose autrement, par une épreuve chronométrée : l'indice reste la mesure
  de tous les jours, l'examen la vérification.
