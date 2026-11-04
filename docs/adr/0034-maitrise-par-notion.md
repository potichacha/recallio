# 0034. La maîtrise par notion

- Statut : accepté
- Date : 2026-11-04

## Contexte

La gamification v2 (ADR 0025, phase 3 ter) commence par la **maîtrise par notion**, qui mesure deux choses : la mémoire
(l'état FSRS des cartes) et la diversité des formats réussis. Elle alimentera ensuite la carte des compétences, l'XP,
les succès et le profil public.

Deux mesures existent déjà, et la maîtrise doit rester cohérente avec elles :
- **la notion acquise** du parcours (ADR 0032) : 5 cartes réussies au moins une fois, ou toutes si la notion en a
  moins. C'est la règle de déblocage des prérequis ;
- **la carte maîtrisée** du tableau de bord : une carte en révision dont la stabilité atteint 21 jours (le seuil des
  cartes « matures » d'Anki).

Le niveau doit pouvoir s'expliquer à l'utilisateur en une phrase (« il te manque 2 cartes et un format »), redescendre
quand il oublie, et se calculer à partir des données existantes.

## Options envisagées

- **Un pourcentage** (la probabilité moyenne de se souvenir des cartes de la notion) : précis, mais opaque. Il baisse
  chaque jour sans que l'utilisateur ne fasse rien, et rien ne dit comment le faire monter.
- **Un score cumulé** (des points par carte réussie) : il mesure l'effort, pas la mémoire, et ne redescend jamais.
- **Des niveaux à seuils**, construits sur les deux mesures existantes (retenu) : chaque niveau a une condition simple,
  et la suivante se lit directement.

## Décision

**Cinq niveaux** (`MasteryLevel`), du moins au plus avancé. Chacun suppose le précédent.

| Niveau | Condition |
|---|---|
| Nouvelle | Aucune carte de la notion vue. |
| Découverte | Au moins une carte vue. |
| Acquise | 5 cartes réussies au moins une fois (notées Bien ou Facile), ou toutes si la notion en a moins : la règle du parcours (ADR 0032), inchangée. |
| Maîtrisée | 5 cartes maîtrisées (en révision, stabilité d'au moins 21 jours), dans au moins 3 formats différents. |
| Experte | 5 cartes retenues sur la durée (stabilité d'au moins 90 jours), dans au moins 3 formats, et chaque format de la notion réussi au moins une fois. |

- **Les petites notions** : une notion de moins de 5 cartes demande toutes ses cartes ; une notion qui retient moins de
  3 formats demande tous ses formats.
- **Experte** demande aussi d'avoir réussi chaque format de la notion : l'exercice de code s'il y en a un, le format
  le plus exigeant.
- **Acquise ne se perd pas** : le parcours ne reverrouille pas une notion. **Maîtrisée et experte se perdent** dès
  qu'une carte est oubliée : notée « À revoir », elle repasse en réapprentissage avec une stabilité faible, et ne compte
  plus.
- **Le calcul se fait à la demande**, sans nouvelle table : à partir de l'état FSRS des cartes (`review_state`) et des
  réussites du journal (`review_log`). Les règles sont dans le domaine du module `review`, en Java pur
  (`NotionMastery`), à côté de `LearningPath`, dont elles reprennent la règle « acquise ».
- **Une seule définition de la carte maîtrisée** : `ReviewState.isStable(jours)`, partagée par le tableau de bord et la
  maîtrise.
- **L'API** : la carte du parcours (`GET /api/v1/learning-paths/{sujet}`) donne pour chaque notion son niveau
  (`mastery`) et ce qui mène au suivant (`nextLevel`) : les cartes qui comptent déjà et celles qu'il demande, les
  formats réunis et ceux qu'il demande, et pour « experte » les formats encore jamais réussis. La PWA dit ainsi ce qui
  manque sans connaître aucun seuil. Elle range les notions par domaine puis par chapitre, et compte les notions de
  chaque niveau (`counts`, des comptes cumulatifs : une notion experte compte aussi comme maîtrisée), pour le sujet et
  pour chaque domaine. Le tableau de bord (`GET /api/v1/me/review-stats`) donne les mêmes comptes pour chaque sujet
  suivi. La PWA en fait la carte des compétences.
- **L'API publique de `content`** donne le format de chaque carte active (`ActiveCard.type`), pour compter les formats
  sans charger le texte des cartes.

## Conséquences

- Aucune migration : la maîtrise ne stocke rien. Changer un seuil change le niveau de tous, sans reprise de données.
- La maîtrise ne voit pas une longue absence tant que les cartes ne sont pas révisées : une carte en retard compte
  encore. Si cela se révèle trompeur, on pourra exiger une probabilité de rappel actuelle minimale.
- Atteindre « maîtrisée » demande quelques semaines de révisions réussies, et « experte » plusieurs mois : ces niveaux
  récompensent la mémoire durable, pas le volume d'une journée.
- Les étapes suivantes de la phase 3 ter s'appuient sur ce niveau : la carte des compétences dans la PWA, puis l'XP,
  les succès et le profil public.
