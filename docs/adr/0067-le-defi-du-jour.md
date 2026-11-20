# 0067. Le défi du jour : cinq questions par sujet, les mêmes pour tous, une tentative

- Statut : accepté
- Date : 2026-11-20
- Prolonge : ADR 0044 (l'examen blanc), ADR 0045 (les cartes de partage), ADR 0050 (l'essai sans compte), ADR 0056
  (les données personnelles), ADR 0063 (la fin de l'auto-évaluation)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 26

## Contexte

Une raison de revenir chaque jour, et de faire venir ses amis : « J'ai fait 4/5 au défi Go du jour, à ton tour ». C'est
le principe de Wordle : le même jeu pour tout le monde, une seule tentative, un résultat qu'on partage sans dévoiler la
solution.

Ce qui existe déjà :

- **l'examen blanc** (ADR 0044) tire des questions corrigées par le serveur, les garde, et ne touche ni à FSRS, ni à
  l'XP, ni à la cote des cartes (ADR 0046) : il ne publie aucun événement ;
- **les défis de groupe** (ADR 0020) sont une série commune aux membres d'un groupe, dans le module `group`. Le défi du
  jour s'appelle partout `daily challenge` (tables `daily_challenge…`, route `/daily/…`) pour ne jamais les confondre ;
- **les cartes de partage** (ADR 0045) sont dessinées dans le navigateur, et rien ne part sans un geste de
  l'utilisateur ;
- **toutes les cartes des dossiers de sujet ont leurs choix** (ADR 0063) : `Grading.CHOICES_OR_BLANKS` choisit celles que
  le serveur corrige sans exécuter de code.

## Options envisagées

**Un défi pour tous les sujets, ou par sujet** :

- **Un seul défi, tous sujets mêlés** : un développeur Java tomberait sur des questions de Python ou de C++.
- **Un défi par sujet** (retenu) : on joue son langage, et on le partage à ceux qui le pratiquent.

**Le jour** :

- **Le jour UTC, pour tous** : le défi changerait à 1 h du matin à Paris, et au milieu de l'après-midi à l'autre bout
  du monde.
- **Le jour de chacun, dans son fuseau** (retenu, comme Wordle) : à Paris et à Montréal, on joue chacun « le défi Go du
  20 novembre » à son 20 novembre.

**Les questions** :

- **Tirées à chaque demande, à partir d'une graine** (le sujet et le jour) : rien à stocker, mais une correction du
  contenu dans la journée (une carte retirée, une notion ajoutée) changerait le défi de ceux qui jouent après.
- **Tirées à la première demande du jour, puis gardées** (retenu) : tout le monde a les mêmes questions, quoi qu'il
  arrive au contenu.

**Le compte** :

- **Jouable sans compte** : une seule tentative ne se garantit pas dans un navigateur, et le score ne compterait pas
  dans les chiffres du jour.
- **Un compte pour jouer** (retenu) : le visiteur arrivé par un lien partagé voit ce qu'est le défi, puis se connecte ou
  fait l'essai sans compte (ADR 0050).

**La récompense** :

- **De l'XP** : le défi deviendrait une façon de gagner de l'XP, sur des cartes déjà vues ou pas.
- **Pas d'XP pour l'instant** (retenu, comme l'examen blanc) : la récompense est le score partagé. À revoir avec les
  quêtes si la mesure du parcours (ADR 0064) le demande.

## Décision

1. **Le défi** : un par sujet publié et par jour, jamais pour un brouillon. Il est tiré à la première demande du jour
   pour ce sujet, puis gardé (migration V32) :
   - `daily_challenge` : `id` (UUID v7), `subject_slug`, `day` (`date`), `created_at`, unique sur (`subject_slug`, `day`).
     Deux premières demandes simultanées : `insert … on conflict do nothing`, puis relecture ; les deux ont le même ;
   - `daily_challenge_question` : les cartes dans l'ordre, par position. Comme l'examen blanc, la carte et non sa
     version : le module `content` ne sert que la version courante. Une carte retirée depuis le tirage n'est plus
     posée, et le défi se finit et se compte sur les autres ;
   - comme les autres tables du module `review`, pas de clé étrangère vers les cartes ni vers les utilisateurs.
2. **Les questions** (`DailyChallengeDraw`, Java pur), cinq, de difficulté croissante : deux de notions débutantes, deux
   intermédiaires, une experte (le niveau des notions du référentiel). Un niveau qui manque se complète par le plus
   proche, et l'ordre reste croissant. Des notions toutes différentes, de domaines différents autant que possible. Des
   cartes corrigées par le serveur sans exécuter de code (`Grading.CHOICES_OR_BLANKS`, comme l'examen blanc), et aucune
   carte des 30 derniers défis du sujet, tant qu'il reste assez de notions sans elles. Le hasard vient d'une graine
   tirée de l'identifiant du défi, comme pour l'examen blanc et les duels. Un sujet qui n'a pas cinq notions avec de
   telles cartes n'a pas de défi : 422 `daily-challenge.unavailable`.
3. **La tentative** :
   - une réponse par question, définitive, corrigée tout de suite (le bon choix et l'explication, comme
     l'entraînement) : `daily_challenge_answer` (défi, utilisateur, position, juste ou non, `answered_at`, `request_id`
     unique). Seul le verdict est gardé, pas la réponse ;
   - idempotente (CLAUDE.md, 4.4) : la même requête renvoyée rend la même correction ; une autre réponse à une question
     déjà répondue est refusée (409 `daily-challenge.already-answered`) ;
   - on peut s'arrêter et reprendre. Un défi commencé avant minuit se finit après : une réponse est acceptée pour le
     défi du jour de l'utilisateur, ou pour celui de la veille s'il l'avait commencé ; jamais pour un autre (409
     `daily-challenge.closed`). Tant qu'il n'est pas fini, le défi de la veille passe avant celui du jour ;
   - le défi est fini quand chaque question a sa réponse ; le score est le nombre de justes.
4. **Les chiffres du jour**, une fois son propre défi fini (avant, ils influenceraient) : le nombre de joueurs qui l'ont
   fini, le score moyen à une décimale, la répartition des scores. Agrégés en SQL, sans aucun nom.
5. **L'API**, dans le module `review`, pour l'utilisateur connecté seulement :
   - `GET /api/v1/me/daily-challenges` : pour chaque sujet publié qui a un défi, les sujets suivis d'abord, le jour,
     l'état (`NOT_STARTED`, `IN_PROGRESS`, `DONE`) et le score s'il est fini. Elle ne tire aucun défi ;
   - `GET /api/v1/me/daily-challenges/{sujet}` : le défi du jour (tiré s'il le faut), ses questions sans leurs
     réponses, les réponses déjà données avec leur correction, l'état, le score et les chiffres du jour s'il est fini ;
     ou celui de la veille, s'il est à finir ;
   - `POST /api/v1/me/daily-challenges/{défi}/answers`, corps `{ requestId, position, answer }` : la correction, l'état,
     le score et, si c'est la dernière réponse, les chiffres du jour ;
   - les refus : 404 `daily-challenge.not-found` et `daily-challenge.not-a-question`, 409 `daily-challenge.closed` et
     `daily-challenge.already-answered`, 422 `daily-challenge.unavailable` ; une réponse qui ne convient pas à la
     question, 422 `review.answer-mismatch`, comme ailleurs dans le module.
6. **Ni FSRS, ni l'XP, ni la cote des cartes** : le défi ne passe pas par le journal de révision et ne publie aucun
   événement, comme l'examen blanc.
7. **La PWA** :
   - la page `/daily/{sujet}`, publique pour qu'un lien partagé s'ouvre chez un visiteur, qui y lit ce qu'est le défi et
     est invité à se connecter ou à faire l'essai ; `robots.txt` l'écarte des moteurs (interactive, elle change chaque
     jour) ;
   - pour un utilisateur : les questions une à une (les composants de l'entraînement), la correction après chaque
     réponse, puis le résultat : le score, les marques des questions (une icône et un texte, jamais la couleur seule),
     les chiffres du jour, le partage, et « Reviens demain ». Revenir sur la page montre le résultat ;
   - le partage (ADR 0045) : l'image dit « Défi du jour · Go », le score, le jour et les marques (un rond plein coché ou
     un rond vide barré), jamais les réponses ; le texte, « J'ai fait 4/5 au défi Go du jour sur Recallio. À ton tour : »,
     suivi du lien du défi ;
   - la page Jouer liste les défis et leur état ; l'accueil a une entrée discrète vers celui du sujet qu'il met en
     avant.
8. **Les données personnelles** (ADR 0056) : `ReviewPersonalData` exporte les réponses de l'utilisateur et les efface
   avec son compte. Les défis eux-mêmes, les questions du jour, ne sont à personne : ils restent.

## Conséquences

- ➕ Une raison de revenir chaque jour, et un résultat qui se partage sans rien dévoiler : le lien amène un ami sur le
  même défi.
- ➕ Une réponse ratée apprend quelque chose : la correction et l'explication arrivent tout de suite, avec « Revoir la
  leçon ».
- ➖ **Le défi ne compte ni pour FSRS ni pour la série** : jouer le défi ne remplace pas la révision du jour. C'est voulu :
  il mesure, il n'entraîne pas la mémoire, et une série doit venir de révisions espacées.
- ➖ **Les chiffres du jour sont ceux de tous les fuseaux** : un défi du 20 novembre se joue pendant 50 heures autour du
  monde, et ses chiffres grandissent tout ce temps. Ils sont agrégés et ne disent le score de personne ; supprimer son
  compte retire ses réponses des chiffres.
- ➖ **Le défi de la veille passe avant celui du jour** tant qu'il est commencé et pas fini : qui s'arrête à la question 3
  avant minuit la retrouve le lendemain, puis enchaîne avec le défi du jour. Un défi de l'avant-veille, lui, est fermé.
- ➖ Un sujet de moins de cinq notions avec des questions corrigées par le serveur n'a pas de défi : les sujets décrits
  par un document Markdown, en brouillon, n'en ont pas.
- 🔁 Une table par défi et par jour, cinq questions chacune : quelques milliers de lignes par an et par sujet, sans
  nettoyage pour l'instant.
