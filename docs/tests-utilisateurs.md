# Tests avec cinq utilisateurs : protocole et grille d'observation

Ce document sert à mener, pendant la bêta, cinq séances d'observation de Recallio (audit du 2026-11-10, élément 56).
Il est écrit pour qui n'a jamais fait de test utilisateur : il dit qui inviter, quoi dire, quoi regarder, et quoi faire
de ce qu'on a vu.

## Pourquoi cinq, et pourquoi observer

- Les tests automatiques disent que l'application **fonctionne**. Ils ne disent pas si quelqu'un qui la découvre
  **comprend** quoi faire.
- Cinq personnes suffisent à faire ressortir la plupart des obstacles qui reviennent : au-delà, on revoit surtout les
  mêmes. Mieux vaut cinq séances, des corrections, puis cinq autres, que quinze séances d'un coup.
- On **regarde quelqu'un faire**, on ne lui demande pas son avis. Ce que les gens disent aimer et ce qui les bloque
  sont deux choses différentes.

## Qui inviter

Cinq personnes qui **n'ont jamais vu Recallio**, réparties sur les trois publics de l'application (CLAUDE.md, 5.5) :

| Profil | Combien | Ce qu'on apprend d'eux |
|---|---:|---|
| Prépare un entretien technique dans les mois qui viennent | 2 | le parcours principal : essayer, s'inscrire, réviser chaque jour |
| Développeur qui veut apprendre un second langage | 2 | le Tour, l'exécution du code, la validation des notions |
| Développeur expérimenté, qui entretient son niveau | 1 | la difficulté des questions, le classé, ce qui manque |

- Au moins **deux séances sur téléphone** : c'est l'usage visé.
- Si possible, une personne qui utilise un **lecteur d'écran** ou la navigation au clavier : elle remplace alors la
  liste de contrôle manuelle de `docs/accessibilite.md` par un usage réel.
- Pas de collègue qui connaît le projet, pas de proche qui voudra faire plaisir.

## Avant la séance

- **Durée** : 45 minutes, dont 30 de tâches.
- **Lieu** : en personne, ou en visio avec partage d'écran. Sur téléphone, une visio depuis le téléphone lui-même, ou
  une seconde caméra sur l'écran.
- **Matériel** : l'environnement de bêta, un compte neuf par personne, ce document, de quoi noter. Idéalement une
  seconde personne qui note pendant que tu parles.
- **Accord** : demander par écrit, avant, l'accord pour enregistrer l'écran et la voix. Dire à quoi sert
  l'enregistrement (relire la séance), qui le verra (toi), et quand il sera effacé (un mois après). Sans accord, on
  n'enregistre pas : on note.
- **Données** : aucune donnée de la personne ne sort de la séance. Dans les notes, P1 à P5, jamais un nom.

## Ce qu'on dit en ouvrant

À lire tel quel, pour que les cinq séances commencent de la même façon :

> Merci d'être là. Je te montre une application sur laquelle je travaille, et j'ai besoin de voir comment quelqu'un
> qui la découvre s'en sert.
>
> Ce n'est pas toi qu'on teste, c'est l'application. Si quelque chose ne marche pas ou n'est pas clair, c'est elle qui
> a un problème, et c'est exactement ce que je cherche.
>
> Je vais te donner quelques petites choses à faire. Pendant que tu les fais, dis à voix haute ce que tu regardes, ce
> que tu cherches, ce que tu t'attends à voir. Je ne t'aiderai pas, même si tu me le demandes : ce n'est pas par
> impolitesse, c'est pour voir ce qui se passerait si je n'étais pas là.
>
> Tu peux arrêter quand tu veux, sans avoir à dire pourquoi.

## Les tâches

On donne la tâche, puis on se tait. Chaque tâche est formulée comme un **but**, jamais comme un chemin : « tu veux
savoir si tu es prêt », pas « clique sur Révisions ».

| N° | Ce qu'on dit | Réussi quand | Temps indicatif |
|---|---|---|---:|
| 1 | « Voici la page d'accueil. Sans rien toucher, dis-moi ce que tu comprends de ce que fait cette application, et pour qui. » | la personne parle de révision pour des entretiens techniques | 1 min |
| 2 | « Tu veux voir ce que ça vaut avant de créer un compte. » | elle répond à au moins une question sans compte | 3 min |
| 3 | « Ça te plaît, tu veux garder ce que tu fais. » | elle a un compte et arrive sur son accueil | 3 min |
| 4 | « Tu as dix minutes devant toi et tu veux réviser. » | elle lance une session et répond à cinq cartes | 6 min |
| 5 | « Tu t'es trompé sur une question et tu veux comprendre pourquoi. » | elle lit l'explication, ou ouvre la leçon, ou retrouve son carnet d'erreurs | 3 min |
| 6 | « Tu veux apprendre ce langage depuis le début, en écrivant du code. » | elle ouvre le Tour et exécute un programme | 5 min |
| 7 | « Tu veux savoir où tu en es, et si tu es prêt pour un entretien. » | elle trouve sa progression ou son indice de préparation | 3 min |
| 8 | « Tu préfères une interface sombre. » | elle change le thème | 2 min |
| 9 | « Tu veux que l'application ne garde plus rien sur toi. » | elle trouve la suppression du compte, sans aller au bout | 2 min |

- Pour le profil « apprendre un langage », passer la tâche 6 avant la tâche 4.
- Si une tâche dépasse le double de son temps, dire « on passe à la suite » et la noter comme **échec**. Ne pas
  montrer la solution : la tâche suivante en dépend rarement, et une explication fausserait la suite.

## Ce qu'on note

Une ligne par tâche et par personne. On note **ce qu'on voit et ce qu'on entend**, pas ce qu'on en déduit.

| Tâche | Résultat | Temps | Ce que la personne a fait | Ce qu'elle a dit | Où elle a hésité ou s'est trompée |
|---|---|---:|---|---|---|
| 1 | réussi / réussi avec peine / échec | | | | |
| 2 | | | | | |

**Réussi avec peine** : la personne y arrive, mais après une erreur, un retour en arrière ou une longue hésitation.

À surveiller en particulier, parce que ce sont les paris de la refonte (ADR 0058) :

- **La page publique** : comprend-elle l'application avant de défiler ? Répond-elle à la question posée dans la page ?
- **« Valider »** : coche-t-elle un choix puis cherche-t-elle le bouton, ou attend-elle que le choix suffise ?
- **Le verdict** : lit-elle l'explication, ou passe-t-elle tout de suite à la carte suivante ?
- **Rico** : le remarque-t-elle ? Sourit-elle, ou est-ce qu'il l'agace ?
- **L'accueil connecté** : sait-elle quoi faire en premier ? Comprend-elle l'anneau et le chemin ?
- **Les mots** : « notion », « thème », « Tour », « classé », « gel de série » sont-ils compris sans explication ?
- **Sur téléphone** : la barre du bas, la taille des cibles, l'éditeur de code.

## Après les tâches

### Trois questions ouvertes

1. « Qu'est-ce qui t'a le plus gêné ? »
2. « Qu'est-ce que tu t'attendais à trouver et que tu n'as pas trouvé ? »
3. « Est-ce que tu reviendrais demain ? Qu'est-ce qui te ferait revenir, ou pas ? »

### Le questionnaire SUS

Dix affirmations, notées de 1 (pas du tout d'accord) à 5 (tout à fait d'accord). C'est un questionnaire standard :
il donne un score sur 100 qu'on peut comparer d'une série de tests à la suivante.

1. Je pense que j'utiliserais cette application régulièrement.
2. J'ai trouvé cette application inutilement compliquée.
3. J'ai trouvé cette application facile à utiliser.
4. Je pense que j'aurais besoin de l'aide de quelqu'un pour utiliser cette application.
5. J'ai trouvé que les différentes fonctions de cette application étaient bien intégrées.
6. J'ai trouvé qu'il y avait trop d'incohérences dans cette application.
7. Je pense que la plupart des gens apprendraient très vite à utiliser cette application.
8. J'ai trouvé cette application très lourde à utiliser.
9. Je me suis senti en confiance en utilisant cette application.
10. J'ai dû apprendre beaucoup de choses avant de pouvoir utiliser cette application.

**Calcul** : pour les affirmations impaires, la note moins 1 ; pour les paires, 5 moins la note. On additionne les dix
résultats et on multiplie par 2,5. Autour de 68, c'est la moyenne des produits testés ; au-dessus de 80, c'est très
bon ; en dessous de 50, il y a un problème sérieux. Avec cinq personnes, le score est une indication, pas une mesure.

## Après les cinq séances

1. **Lister les obstacles**, un par ligne, avec les personnes qui l'ont rencontré.

| Obstacle | Tâche | Personnes | Gravité |
|---|---|---|---|
| ne voit pas le bouton « Valider » sous la liste des choix | 4 | P1, P3, P4 | bloquant |

2. **Donner une gravité** à chacun :

| Gravité | Définition | Ce qu'on en fait |
|---|---|---|
| Bloquant | la personne n'a pas pu finir la tâche | corrigé avant d'ouvrir la bêta plus largement |
| Gênant | elle a fini, mais après une erreur ou une longue hésitation | corrigé dans les semaines qui suivent |
| Mineur | elle l'a remarqué, sans en être ralentie | noté, corrigé si c'est simple |

3. **Trier** : un obstacle rencontré par trois personnes sur cinq passe avant un obstacle rencontré par une seule,
   même plus grave en apparence. Un obstacle rencontré par une seule personne n'est pas ignoré pour autant s'il est
   bloquant.
4. **Écrire le bilan** dans `docs/retro/tests-utilisateurs.md` : le tableau des résultats par tâche, le score SUS, les
   obstacles triés, et ce qu'on décide. Chaque obstacle retenu devient une étape de la feuille de route ; un défaut
   qu'une machine peut voir reçoit aussi son test.
5. **Recommencer** avec cinq nouvelles personnes une fois les obstacles bloquants corrigés : c'est la seule façon de
   savoir si la correction a marché.

## Ce que ces tests ne disent pas

- **Si les gens reviennent** : c'est la mesure de la rétention à 1, 7 et 30 jours qui le dit (page des admins).
- **Si le contenu est juste** : ce sont les signalements d'erreurs et la relecture de chaque pull request de contenu.
- **Laquelle de deux versions marche le mieux** : il faudrait un test A/B, donc bien plus de cinq utilisateurs (audit,
  élément 57, après la bêta).
