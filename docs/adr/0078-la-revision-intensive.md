# 0078. La révision intensive : un tirage par graine, sans état, et un compteur du jour

- Statut : accepté
- Date : 2026-11-26
- Prolonge : ADR 0038 (l'entraînement libre, non enregistré), ADR 0031 (une carte par notion et par jour), ADR 0051 et
  0063 (chaque carte se corrige sur ses choix), ADR 0056 (`PersonalData`), ADR 0076 (`UsageStatistics`), ADR 0074 (la
  politique de confidentialité dit ce que le code garde), ADR 0077 (la refonte « trois portes », étape 6)
- Origine : la refonte « trois portes », étape 6 ; cet ADR couvre le serveur, l'écran vient dans la PR suivante

## Contexte

La porte « S'entraîner » (ADR 0077) promet des questions en boucle, tous sujets mélangés, comme en entretien : on
choisit ses sujets et le poste visé (junior, confirmé, senior), et les questions s'enchaînent jusqu'à ce qu'on
s'arrête, avec un bilan par notion à la fin.

L'entraînement libre (ADR 0038) fait déjà la moitié du chemin : il corrige sans rien enregistrer. Mais il porte sur
une seule notion et rend toutes ses cartes d'un coup. Ici, cinq sujets peuvent compter plus de 25 000 cartes.

Quatre contraintes :

- **la répétition espacée ne change pas** (ADR 0077) : ni une leçon, ni la révision intensive, ni une partie
  n'écrivent dans FSRS ;
- **une limite quotidienne** attend les comptes gratuits (étape 9, le droit « Plus ») : le serveur doit savoir combien
  de réponses un compte a fait corriger aujourd'hui ;
- **la page « Bêta »** (ADR 0076) doit dire si la fonctionnalité sert ;
- **la politique de confidentialité** (ADR 0074) dit ce que le code garde : moins on garde, moins il y a à dire, à
  exporter et à effacer.

## Options envisagées

**Où ranger les réponses** :

- **Dans le journal de révision et FSRS**, comme une session : la séance compterait pour l'XP et la série. Mais
  l'ADR 0031 existe parce que des cartes sœurs enchaînées reçoivent des notes flatteuses, et l'ADR 0038 a déjà refusé
  cette exception. Une séance de cent questions repousserait cent échéances.
- **Nulle part** (retenu) : un entraînement libre, comme celui de l'ADR 0038.

**Comment ne pas reposer deux fois la même carte** :

- **Une séance sur le serveur** (une table `drill_session`, avec ses cartes déjà posées) : le serveur sait tout, mais
  il garde une ligne par question, à exporter, à effacer et à purger, et une séance abandonnée reste à nettoyer.
- **La liste des cartes déjà vues envoyée par le client** : sans état, mais une requête qui grossit sans fin.
- **Une graine et une position** (retenu) : le client choisit une graine au début de la séance. Elle fixe l'ordre de
  toutes les cartes éligibles ; `offset` dit où il en est. Le serveur ne garde rien, et une même carte ne revient
  qu'une fois toutes les autres posées.

**Ce que le serveur garde pour la limite quotidienne** :

- **Un journal des réponses** (la carte, le résultat, l'heure) : il permettrait un bilan côté serveur et des
  statistiques par carte. C'est une donnée personnelle de plus, détaillée, pour un besoin que l'écran couvre déjà : il
  calcule le bilan avec ce que chaque correction lui renvoie.
- **Un compteur par compte et par jour** (retenu) : le nombre de réponses, et combien étaient justes. Ni la carte, ni
  la réponse, ni l'heure.
- **Rien, et la limite côté client** : elle se contourne en rechargeant la page.

**Les niveaux** : le référentiel donne à chaque notion un niveau et une priorité (ADR 0025). Trois postes en font trois
filtres, sans donnée nouvelle. L'autre piste, la cote de difficulté des cartes (ADR 0046), apprend des réponses des
joueurs : avant la bêta, elle n'a encore rien appris.

## Décision

La révision intensive est un entraînement libre (ADR 0038) sur plusieurs sujets, dans le module `review`, sous
`/api/v1/me/drill`. Il faut un compte.

### Les cartes éligibles

Les cartes actives qui se corrigent sur leurs choix (ADR 0051) et qui ont une notion, hors exercices de code : on y
répond d'un geste, sans exécuter de code. Un code à trous sans choix n'en fait pas partie.

Les sujets sont ceux que l'appelant peut voir : les sujets publiés, et les brouillons pour un admin, qui relit ainsi un
nouveau sujet en conditions réelles. Un brouillon demandé par un autre répond comme un sujet inconnu (404).

| Poste | Niveaux des notions | Priorités |
|---|---|---|
| `JUNIOR` | débutant, intermédiaire | 1 et 2 |
| `CONFIRMED` | intermédiaire, expert | 1 à 3 |
| `SENIOR` | intermédiaire, expert | 1 à 4 |

### Le tirage (`DrillDraw`, dans le domaine)

`GET /api/v1/me/drill/questions?subjects=…&level=…&seed=…&offset=…&size=…` : de 1 à 5 sujets, une page de 10
questions par défaut, 20 au plus.

- Les cartes éligibles sont triées par identifiant, puis mélangées avec la graine : l'ordre ne dépend que d'elle.
- Un **tour** pose chaque carte une fois. Il prend une carte de chaque notion, puis une deuxième de chacune, et ainsi
  de suite : deux cartes d'une même notion sont séparées par autant de questions qu'il reste de notions.
- **Une seule carte par notion sur une page.** La page s'arrête avant une carte dont la notion y est déjà, ou à la fin
  du tour. Elle peut donc être plus courte que demandé, et la réponse donne `nextOffset`, l'offset de la page
  suivante : aucune carte n'est sautée.
- **Sans fin** : passé le total, un nouveau tour commence, dans un autre ordre tiré de la même graine et du numéro du
  tour.
- La réponse donne les questions comme l'entraînement les donne (sans leur corrigé), avec le sujet et la notion de
  chacune, et le nombre de cartes éligibles.

### La correction

`POST /api/v1/me/drill/checks`, avec la carte et les choix cochés : la correction de l'entraînement
(`AnswerCorrection`), le sujet et la notion de la carte (de quoi tenir le bilan et ouvrir « Revoir la leçon »), et le
nombre de réponses du jour. La correction ne dépend ni des sujets ni du poste de la séance : toute carte éligible que
l'appelant peut voir se corrige.

**Rien n'est enregistré** dans le journal de révision, FSRS, l'XP ni la série, et aucun événement n'est publié.

### Le compteur du jour

`drill_activity (user_id, local_date, answers, correct)`, une ligne par compte et par jour, mise à jour à chaque
correction dans la même transaction. Le jour est celui de l'utilisateur, dans son fuseau, d'après l'horloge injectée.
`GET /api/v1/me/drill/usage` rend celui d'aujourd'hui.

- `ReviewPersonalData` l'exporte et l'efface avec le compte (ADR 0056).
- `UsageFeature.INTENSIVE_DRILL` compte, sur la page « Bêta », les comptes qui ont au moins une ligne sur la période
  (ADR 0076).
- La politique de confidentialité le dit, à la ligne « Ta progression » (ADR 0074).

### L'API publique du contenu

Le module `review` ne lit pas les tables de `content` (ADR 0001). Trois ajouts à sa façade :

- `SubjectScope` (`PUBLISHED`, `WITH_DRAFTS`) : ce que l'appelant peut voir ;
- `CardDirectory.choiceCards(sujets, scope)` : les cartes à choix de plusieurs sujets, avec le niveau et la priorité
  de leur notion, et `CardDirectory.currentVersions(cartes, scope)` ;
- `SubjectDirectory.requireVisible(slug, scope)`.

Le reste de la façade ne voit toujours que les sujets publiés.

## Conséquences

- **La révision intensive ne fait pas progresser** : ni XP, ni série, ni échéance. L'écran le dit (« Cet entraînement
  ne change pas tes révisions du jour »). Les cartes reviennent dans les révisions espacées comme les autres.
- **Le bilan est à l'écran** : le serveur ne sait pas quelles cartes une séance a posées. Une séance ne se reprend pas
  sur un autre appareil, sauf à y porter la graine et l'offset.
- **Chaque page relit les cartes éligibles** des sujets demandés (leur identifiant, leur notion, son niveau et sa
  priorité) pour les mélanger : quelques dizaines de milliers de lignes courtes pour cinq grands sujets. Si la mesure
  le demande, un cache par sujet suffira, le catalogue ne changeant qu'à l'import.
- **Un import en cours de séance change l'ordre** : une carte ajoutée ou retirée déplace les autres dans le tour. Une
  carte peut alors revenir ou manquer une fois. C'est le prix de l'absence d'état, accepté pour un entraînement.
- **La correction n'est pas idempotente** : une requête rejouée après une coupure compte deux réponses. La section 4.4
  de `CLAUDE.md` l'exige de ce qui modifie la progression, que ce compteur ne touche pas ; la limite de l'étape 9
  décidera si un écart d'une réponse compte.
- **Le compteur ne distingue pas les sujets** : la page « Bêta » sait qui s'entraîne, pas sur quoi. Une colonne de plus
  le dirait, le jour où la question se pose.
- **Un admin s'entraîne sur les brouillons**, ce que ni la file de révision ni l'entraînement d'une notion ne
  permettent : `SubjectScope` pourra les y ouvrir.
- **Ce qui attend** : la limite quotidienne des comptes gratuits et le droit « Plus » (étape 9), qui liront ce
  compteur ; l'écran (les réglages, la question, « formuler d'abord », le bilan), dans la PR suivante.
