# 0040. Les quêtes et les succès

- Statut : accepté
- Date : 2026-11-05

## Contexte

La gamification v2 (ADR 0025, CLAUDE.md 6.3) prévoit des **quêtes du jour et de la semaine qui font varier les formats**
et des **succès débloqués une seule fois**. Le Tour (ADR 0037) s'intercale avant elles pour qu'elles puissent aussi le
récompenser : « lire 3 pages », « valider une notion ».

Deux règles du produit encadrent le tout :

- la gamification ne pousse jamais à bâcler : rien ne récompense la vitesse ;
- l'XP est un registre append-only (`points_ledger`, ADR 0035) : un total est une somme, jamais un compteur modifié.

## Options envisagées

- **Choisir les quêtes chaque jour et les stocker** : une tâche planifiée, à minuit de chaque fuseau, ou un tirage à la
  première lecture. Il faudrait gérer les utilisateurs qui ne viennent pas, et le tirage concurrent de deux requêtes.
- **Les mêmes quêtes pour tout le monde** : simple, mais toujours dans le même ordre, et sans variété d'un utilisateur à
  l'autre dans un groupe.
- **Un choix calculé, déterministe** (retenu) : les quêtes d'une période se déduisent de l'utilisateur et de la période.
  Rien à tirer ni à planifier ; le même calcul sert à l'affichage et au suivi.
- Pour les succès, **les recalculer à chaque lecture** depuis l'historique : impossible pour certains (« une série de 7
  jours » se perd quand la série retombe), et coûteux. Ils sont donc **enregistrés au moment où ils se débloquent**.

## Décision

- **Le catalogue** (`Quest`, dans le domaine du module `gamification`) : neuf quêtes, chacune avec sa période, l'événement
  qui la fait avancer, son objectif et son XP.

  | Quête | Période | Objectif | XP |
  |---|---|---|---|
  | Réviser des cartes | jour | 15 | 20 |
  | Réussir des questions corrigées par le serveur | jour | 5 | 25 |
  | Réviser des cartes de code | jour | 3 | 25 |
  | Lire des pages du Tour | jour | 3 | 15 |
  | Valider une notion du Tour | jour | 1 | 20 |
  | Réviser plusieurs jours | semaine | 5 | 60 |
  | Réussir des exercices de code | semaine | 2 | 60 |
  | Réviser des formats différents | semaine | 5 | 50 |
  | Valider des notions du Tour | semaine | 5 | 60 |

- **Le choix** (`QuestBoard`) : trois quêtes par jour, deux par semaine, prises à la suite dans le catalogue de leur
  période, à partir d'un décalage qui dépend de la période et de l'utilisateur. Le jour et la semaine (du lundi au
  dimanche) se lisent dans le fuseau de l'utilisateur, comme le streak.
- **Le suivi** (`QuestTracker`) : un observateur synchrone des événements du module `review` (carte révisée, page du Tour
  ouverte, notion du Tour validée), dans leur transaction, comme l'XP des révisions. Une page compte une fois par jour,
  un format et un jour une fois par semaine (`quest_seen`).
- **La récompense** : une quête terminée écrit son XP dans le registre (`reason = QUEST`, sans sujet), une seule fois. La
  ligne a pour source un identifiant dérivé de l'utilisateur, de la quête et de sa période, que la contrainte d'unicité
  du registre protège des doublons.
- **Les succès** (`Achievement`) : onze succès, de la première carte à la série de 30 jours, au premier exercice réussi,
  à trois sujets révisés, au Tour et aux quêtes. Un observateur les débloque au fil des mêmes événements et les inscrit
  dans la table `achievement`, une ligne par succès débloqué, jamais retirée.
- **L'API** : `GET /api/v1/me/quests` (les quêtes de la période et où en est l'utilisateur) et
  `GET /api/v1/me/achievements` (tous les succès, débloqués ou non).
- **La PWA** : les quêtes sur le tableau de bord, les succès sur le profil.

## Conséquences

- Aucune tâche planifiée : une quête qui n'avance pas n'a pas de ligne, et une nouvelle période part de zéro.
- Les valeurs des énumérations sont stockées par leur nom : renommer une quête ou un succès demande une migration.
  Changer l'ordre du catalogue change les quêtes proposées, mais pas les progrès déjà enregistrés.
- Les quêtes donnent de l'XP sans sujet : elle compte dans le total, pas dans le niveau d'un sujet (ADR 0035).
- Aucune quête ni aucun succès ne dépend du temps de réponse, et l'entraînement libre (ADR 0038) ne fait rien avancer :
  il n'enregistre rien.
- Les succès ajoutés plus tard ne se débloquent qu'à partir de leur arrivée : pas de rattrapage sur l'historique.
