# 0077. La refonte « trois portes » : Apprendre, S'entraîner, Jouer

- Statut : accepté
- Date : 2026-11-26
- Révise : ADR 0052 (la navigation Accueil, Cours, Tour, Révisions et Jouer), ADR 0058 (Rico : son dessin et ses
  poses)
- Prolonge : ADR 0037 (le Tour), ADR 0038 (l'entraînement libre), ADR 0048 et 0063 (la session de révision), ADR 0059
  (le Tour ouvre la révision), ADR 0021 à 0024 (le mode live), ADR 0049 (les règles du design system)
- Origine : une demande du propriétaire, le 2026-11-26, après la préparation du déploiement ; les maquettes sont dans
  `docs/maquettes/trois-portes/`

## Contexte

L'application a cinq onglets (Accueil, Cours, Tour, Révisions, Jouer) et une douzaine de modes : le Tour, le cours, la
révision espacée, l'entraînement libre, l'examen blanc, le carnet d'erreurs, le défi du jour, les groupes, le live, le
classé. Chacun est né d'une étape de la feuille de route, et chacun fonctionne. Personne ne les a encore utilisés.

Le propriétaire veut que le produit se raconte en trois phrases :

- **apprendre** un sujet pas à pas, sur un chemin de leçons courtes ;
- **s'entraîner** en boucle, tous sujets mélangés, comme en entretien ;
- **jouer** à plusieurs : les mêmes questions, un chrono, des points pour la justesse et la vitesse, un classement à
  partager.

Les trois quarts existent déjà : le chemin est le parcours « apprendre » sur les notions du Tour, l'entraînement en
boucle est l'entraînement libre étendu à plusieurs sujets, le jeu est le mode live sorti des groupes. La refonte est
d'abord une façon de présenter l'existant, puis quatre ajouts : la révision intensive, les parties à code, les amis,
et une offre payante.

Le propriétaire a aussi tranché l'ordre : la refonte passe avant la mise en service (étape 7.7 du déploiement).

## Options envisagées

**Quand** :

- **A. Après la bêta**, une fois que la page « Bêta » (ADR 0076) a dit quels modes servent. C'est ce que Claude a
  recommandé : on refond en sachant.
- **B. Avant la mise en service.** Les premiers testeurs découvrent le produit dans sa forme voulue, et leurs retours
  portent sur elle. Le risque : plusieurs semaines de travail sur une intuition, et un déploiement repoussé d'autant.

**Les vies** (une erreur en coûte une, et l'on attend ou l'on paie quand il n'y en a plus) :

- **A. Des vies**, comme levier de l'offre payante.
- **B. Pas de vies.** Le public a une date d'entretien : bloqué une semaine avant, il part. La révision espacée a
  besoin des erreurs, et des vies poussent à éviter les questions difficiles.

**La correction des réponses rédigées** :

- **A. Par une IA** : un coût à chaque réponse, des corrections parfois fausses, et les réponses envoyées à un
  prestataire.
- **B. « Formuler d'abord »** : la question s'affiche sans ses choix ; on écrit ou l'on dit sa réponse, qui ne quitte
  pas le téléphone ; puis les choix apparaissent et le serveur corrige, comme partout (ADR 0051).

**L'offre payante** :

- **A. Le paiement tout de suite** : il demande un statut, des conditions de vente, la TVA et un prestataire.
- **B. Le droit « Plus » et des codes d'accès d'abord**, le paiement après la bêta.

## Décision

**B partout** : la refonte avant la mise en service, sans vies, sans IA, et le droit « Plus » ouvert par des codes
avant tout paiement.

### Ce que deviennent les écrans

| Porte | Ce qu'on y trouve | D'où ça vient |
|---|---|---|
| Accueil | la leçon à continuer, les révisions du jour, les deux autres portes | l'accueil (ADR 0058) |
| Apprendre | le chemin d'un sujet, ses leçons, le cours, les exercices, les révisions du jour | le Tour, le cours, le parcours, la session de révision |
| S'entraîner | la révision intensive, l'examen blanc, le carnet d'erreurs | l'entraînement libre, l'examen blanc, le carnet (ADR 0038, 0044, 0057) |
| Jouer | les parties à code, le défi du jour, le classé, les groupes | le live, le défi du jour, le classé, les groupes |

Le profil s'ouvre depuis l'avatar. Aucune fonctionnalité n'est retirée : celles qui n'ont pas de maquette gardent
leur écran, rangé sous sa porte et passé au nouveau style.

### Les règles

- **Une unité est un chapitre du référentiel, une leçon est une notion.** Le chemin range les leçons dans l'ordre du
  référentiel, comme le sommaire du Tour.
- **Une leçon** montre les pages de la notion dans le Tour (le texte, le programme à exécuter, le « À toi »), puis ses
  questions de validation. La valider, c'est valider la notion dans le Tour : rien ne change dans la règle de l'ADR
  0059. Une notion sans page a pour leçon son cours et ses questions.
- **La répétition espacée ne change pas.** Les « révisions du jour » sont la session de révision (ADR 0048, 0063) :
  les cartes dues d'abord, un quota de nouvelles cartes par sujet, une carte par notion et par jour (ADR 0031). Ni la
  leçon, ni la révision intensive, ni une partie n'écrivent dans FSRS.
- **La révision intensive** est un entraînement libre (ADR 0038) sur plusieurs sujets, sans fin, corrigé par le
  serveur, avec un bilan par notion. Elle fera l'objet de son ADR.
- **Une partie à code** se joue hors d'un groupe, sur un ou plusieurs sujets, et se rejoint par un code à six chiffres.
  Elle reprend le déroulé, le chrono et les points du live (ADR 0022, 0023). Jouer **sans compte** contredit la section
  4.5 de `CLAUDE.md` : c'est une étape à part, avec son ADR.
- **Les amis** sont des données personnelles : le module répond de ses données par `PersonalData` (ADR 0056), le
  profil reste privé par défaut, et la politique de confidentialité est mise à jour dans la même PR (ADR 0074).
- **La ligue de la semaine** classe sur la régularité (les jours où l'objectif est tenu), jamais sur le volume
  (section 6.3).
- **« Plus »** est un droit attaché au compte, avec une date de fin. Pendant la bêta, seuls des codes d'accès l'ouvrent ;
  un admin en crée des lots. Il lève la limite quotidienne de la révision intensive. Ce qu'il débloque d'autre, et son
  prix, se décident avec le paiement : l'examen blanc et le compte à rebours restent gratuits d'ici là.

### Les maquettes font référence

`docs/maquettes/trois-portes/` contient les 35 planches et leur feuille de style (`kit.css`). Chaque étape reprend de
ses planches la structure, les textes, les tailles, les espacements et les couleurs. Trois réserves :

- **là où une maquette contredit une règle, la règle gagne**, comme dans l'ADR 0058 : les tokens du design system, les
  contrastes calculés, `button()`, les états par `LoadingState`, `ErrorState` et `EmptyState`, le garde-fou
  `motion-safe:` ;
- **les couleurs qu'une maquette ajoute** (le bleu de « S'entraîner », les quatre tuiles d'une question en direct)
  entrent dans les tokens, en clair et en sombre, avec leur contraste vérifié par le test existant ;
- **les données des maquettes sont des exemples** : les noms, les scores, les 20 questions gratuites par jour.

Les maquettes n'ont pas été contrôlées sur un rendu : un écart entre une planche et ce qu'elle voulait montrer se
corrige dans le code, et la planche est mise à jour.

### Rico

Rico gagne du volume (une ombre, un ventre plus sombre, un reflet), des bras, des pieds, des joues, et seize poses. Il
reste dessiné avec les tokens, sans dégradé, dans `Rico.tsx`. C'est toujours un dessin de principe : un illustrateur
fera mieux les mains.

## Conséquences

- **Le déploiement attend.** La refonte compte treize étapes, une quinzaine de PR. `main` reste déployable à chaque
  merge : la mise en service peut être avancée à tout moment si le propriétaire change d'avis.
- **La navigation de l'ADR 0052 est remplacée**, et avec elle les parcours de bout en bout qui la suivent.
- **Trois modules arrivent** : les parties à code (dans `live`, ou à côté, à décider dans son ADR), les amis, et le
  droit « Plus » avec ses codes. Chacun a son ADR et ses migrations.
- **La politique de confidentialité change** deux fois : pour les amis, et pour les codes.
- **Le budget de 200 Ko** de JavaScript à l'ouverture (étape 8.3) tient toujours : les seize poses de Rico, toutes
  chargées à l'ouverture, pèsent 2,2 Ko (152,2 Ko mesurés à l'étape 1).
- **Rien n'est mesuré** : on ne sait pas si trois portes valent mieux que cinq onglets. La page « Bêta » le dira, sur la
  nouvelle forme.
- **Ce qui reste hors de la refonte** : le paiement, la correction par IA, les vies.
