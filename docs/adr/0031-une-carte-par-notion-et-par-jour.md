# 0031. Une carte par notion et par jour

- Statut : accepté
- Date : 2026-11-03

## Contexte

La refonte du contenu (ADR 0025) vise 4 à 5 cartes par format et par notion, soit une trentaine de cartes pour une
notion qui retient tous les formats. La file de révision prend les nouvelles cartes dans l'ordre du programme : une
session pourrait enchaîner dix cartes de la même notion. La deuxième se révise alors avec la réponse de la première
encore en tête, ce qui donne une note flatteuse, une échéance trop lointaine, et une session monotone. CLAUDE.md
(phase 3 bis, étape 5) le demande explicitement : « deux cartes d'une même notion pas le même jour ».

## Options envisagées

- **Espacer les sœurs dans la session** (une carte de la notion toutes les N cartes) : la mémoire de travail garde
  encore la réponse au bout de quelques minutes.
- **Décaler l'échéance des sœurs** quand on note une carte : cela modifie l'état FSRS de cartes qu'on n'a pas révisées,
  et fausse le journal qui sert à réentraîner les paramètres (CLAUDE.md, section 6.2).
- **Enterrer les sœurs pour la journée** (retenu) : c'est la règle d'Anki (*bury siblings*). La file ne les propose pas
  aujourd'hui, sans toucher à leur état ; elles reviennent le lendemain.

## Décision

Dans `ReviewQueuePlanner` (domaine, Java pur) :
- **Une seule carte par notion dans la file.** Pour les cartes dues, c'est la plus en retard qui est retenue, et les
  autres restent dues. Pour les nouvelles, c'est la première dans l'ordre du programme.
- **Aucune carte d'une notion dont une autre carte a déjà été révisée aujourd'hui**, depuis minuit dans le fuseau de
  l'utilisateur, d'après le journal de révision.
- **Une carte n'est jamais la sœur d'elle-même.** Notée « À revoir », elle revient dans la journée, et ce réapprentissage
  ne doit pas être bloqué.
- **Les cartes sans notion** (documents Markdown) n'ont pas de sœurs et ne sont pas concernées.
- **Les compteurs suivent la file** : « cartes dues » et « nouvelles » ne comptent que les cartes proposables aujourd'hui.

**Les données :** le module `content` expose la notion de chaque carte par son API publique (`ActiveCard.notionId`,
`CardDirectory.notionsOf`), et le journal de révision donne les cartes révisées depuis un instant.

## Conséquences

- Une notion riche se révise sur plusieurs jours, une carte par jour, ce qui va dans le sens de la répétition espacée.
  Une session puise donc dans plus de notions.
- Avec peu de notions suivies, la file peut être courte certains jours, même avec des cartes dues : c'est voulu.
- Le quota quotidien de nouvelles cartes reste global (ADR 0014). Une seule nouvelle carte par notion et par jour le
  répartit naturellement entre les notions.
- Les défis de groupe et les parties live tirent leurs questions autrement, et ne sont pas concernés.
