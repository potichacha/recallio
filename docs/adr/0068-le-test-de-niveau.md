# 0068. Le test de niveau : deux questions par domaine des bases, pour sauter ce qu'on sait déjà

- Statut : accepté
- Date : 2026-11-20
- Prolonge : ADR 0032 (les parcours), ADR 0034 (la maîtrise), ADR 0037 (le Tour), ADR 0041 (l'indice de
  préparation), ADR 0052 (le premier login guidé par le but), ADR 0056 (les données personnelles), ADR 0059 (le Tour
  avant la révision)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 34

## Contexte

Quelqu'un qui programme déjà et veut apprendre Go suit le parcours « apprendre » (ADR 0052) : chaque notion attend
d'être validée dans le Tour (ADR 0059) et que ses prérequis soient acquis (ADR 0032). Il doit donc traverser les 72
notions débutantes de Go (106 en Java), dont il connaît une bonne partie : les variables, les boucles, les fonctions.
Rien ne lui permettait de dire « ça, je le sais » ; il décroche avant d'arriver à ce qu'il est venu apprendre.

Le test de niveau lui fait **montrer en quelques minutes** ce qu'il sait déjà, et **saute** ces bases dans le Tour et
dans la révision.

« Test » se confondrait avec les tests du code (`LevelTestTest`), et « placement » avec les matchs de placement du
classé (ADR 0046) : le test de niveau s'appelle partout `level check` (`LevelCheck…`, table `known_notion`, route
`/subjects/{sujet}/level-check`) ; l'interface dit « test de niveau ».

## Options envisagées

**Le format** :

- **Une question par notion débutante** : plus de cent questions en Java ; personne ne le finit, et c'est le Tour en
  plus long.
- **Une question par chapitre** (31 en Java) : plus court, mais une seule réponse juste, une fois sur trois au hasard
  avec trois choix, suffirait à sauter tout un chapitre.
- **Deux questions par domaine des bases, avec une règle d'arrêt** (retenu) : deux réponses justes au hasard arrivent
  une fois sur neuf avec trois choix, une fois sur seize avec quatre ; assez rare pour généraliser à tout le domaine.
  Deux domaines ratés de suite arrêtent le test : un débutant a fini en quatre questions, un connaisseur en
  vingt-quatre au plus (une douzaine de domaines, environ cinq minutes).

**L'effet d'un domaine réussi** :

- **Rendre ses notions acquises** : la maîtrise (ADR 0034) et l'indice de préparation (ADR 0041) mentiraient, et FSRS
  doit ne voir que les vraies révisions, espacées.
- **Les valider dans le Tour** : une validation du Tour est une réponse juste aux questions de la notion elle-même, pas
  une généralisation à partir de deux autres ; et une validation reste, alors qu'on doit pouvoir revenir sur un test
  où l'on s'est surestimé.
- **Des notions « connues », à part** (retenu) : une table à part, qui ouvre le parcours et fait passer leurs cartes
  après les autres, sans rien changer d'autre, et qui s'annule d'un geste.

**La session** :

- **Une session gardée**, comme l'examen blanc (ADR 0044) : les questions tirées, les réponses données, un état. Mais le
  test est une révision personnelle : aucune mesure anti-triche (CLAUDE.md, 2.4), et tricher ne nuirait qu'au
  tricheur.
- **Une correction par domaine, sans état** (retenu) : le serveur tire le test à chaque demande, puis corrige un domaine
  d'après les cartes répondues, après avoir vérifié qu'elles sont bien celles d'un tirage de ce domaine. La règle
  d'arrêt est appliquée par la PWA, qui pose les domaines un par un.

**Pour qui** :

- Surtout pour le parcours « apprendre », dont il lève les verrous : le Tour et les prérequis.
- En « préparer un entretien », il ne verrouille rien, mais fait passer les bases connues après les autres notions de
  même priorité : on révise d'abord ce qu'on ne sait pas.

## Décision

1. **Le test** porte sur les notions **débutantes** d'un sujet à référentiel, rangées par **domaine**, dans l'ordre du
   référentiel ; seuls les domaines qui ont une notion débutante avec des questions corrigées sans exécuter de code
   (`Grading.CHOICES_OR_BLANKS`, comme l'examen blanc et le défi du jour) y entrent. Java en a 11, Go 12.
2. **Deux questions par domaine** (`LevelCheckDomain.QUESTIONS_PER_DOMAIN`), tirées de deux notions débutantes
   différentes, de chapitres différents si possible, une carte au hasard de chacune. Un domaine dont une seule notion a
   des questions n'en pose qu'une. Un domaine est **réussi** si toutes ses questions sont justes.
3. **Deux domaines ratés de suite arrêtent le test** (`FAILED_DOMAINS_TO_STOP`, dans la PWA). Les domaines non posés ne
   sont pas connus.
4. **Un domaine réussi rend connues toutes ses notions débutantes**, celles qui n'ont pas été posées comprises : table
   `known_notion` (migration V33), clé (`user_id`, `notion_id`), avec le sujet et l'instant, sur le modèle de
   `tour_progress`, sans clé étrangère vers les autres modules. Une notion connue :
   - **n'attend plus le Tour** en parcours « apprendre » (`LearningPath.awaitsTour`), comme une notion validée ;
   - **compte comme un prérequis satisfait** (`LearningPath.unlocked` : chaque prérequis est acquis **ou connu**). Une
     notion connue dont un prérequis n'est ni acquis ni connu reste verrouillée ;
   - **passe après les autres** dans l'ordre des nouvelles cartes (`NewCardOrder`) : en « apprendre », avant tout le
     reste de la clé (sinon on reverrait deux mille cartes de bases avant la première notion intermédiaire) ; en
     « préparer », après la priorité ; en « entretenir », rien ne change, les débutantes y sont déjà à la fin. Le compte
     à rebours (ADR 0066), qui range ses notions comme le parcours « préparer », suit ;
   - **n'est pas acquise** : la maîtrise, l'indice de préparation, FSRS, l'XP et la cote des cartes ne voient que les
     vraies révisions. Les réponses du test ne sont pas des révisions et ne publient aucun événement.
5. **Refaire le test ajoute**, sans rien retirer : un domaine raté une seconde fois reste connu s'il l'était. **Annuler
   le test** d'un sujet retire toutes ses notions connues.
6. **L'API**, dans le module `review`, pour l'utilisateur connecté seulement :
   - `GET /api/v1/me/level-checks/{sujet}` : le test, tiré à chaque appel, ses domaines dans l'ordre (identifiant,
     titre, nombre de notions débutantes, déjà connu ou non) et leurs questions sans les réponses ; le nombre de
     notions connues du sujet ;
   - `POST /api/v1/me/level-checks/{sujet}/domains/{domaine}`, corps `{ answers: [{ cardId, answer }] }` : les
     corrections, `passed`, et les notions connues du sujet après. Le serveur vérifie que les cartes sont des questions
     de notions débutantes **différentes** de ce domaine, autant que le domaine en pose ; sinon 422
     `level-check.invalid-answers`. Idempotent par nature ;
   - `DELETE /api/v1/me/level-checks/{sujet}` : 204, les notions connues du sujet sont oubliées ;
   - les refus : 404 pour un sujet inconnu ou un brouillon (comme le défi du jour : l'API publique du module `content`
     ne sert que les sujets publiés), 404 `level-check.domain-not-found`, 422 `level-check.unavailable` pour un sujet
     sans notion débutante à questions, 422 `review.answer-mismatch` pour une réponse qui ne convient pas à sa question ;
   - `GET /api/v1/me/tour/{sujet}` donne aussi les notions connues, pour que le sommaire du Tour les marque.
7. **La PWA** :
   - la page `/subjects/{sujet}/level-check`, réservée aux comptes et écartée des moteurs par `robots.txt` :
     l'introduction, les domaines un par un (« Domaine 3 sur 12 : Les collections », les composants de question de
     l'examen blanc), le verdict de chaque domaine avec ses corrections, puis la fin : chaque domaine et son verdict
     (une icône et un texte), les notions sautées, « Continuer le Tour » vers la première notion ni validée ni connue, et
     « Annuler le test », après confirmation ;
   - au premier login, pour « je connais déjà un autre langage », une fois le langage choisi : « Commencer le Tour » ou
     « Passer le test de niveau » (« si tu as déjà des bases ») ;
   - le sommaire du Tour : « Déjà des bases ? Passe le test de niveau », puis « Refaire le test de niveau » ; les notions
     connues y sont marquées « Connue », et « Commencer le Tour » part de la première notion à découvrir.
8. **Les données personnelles** (ADR 0056) : `ReviewPersonalData` exporte `known_notion` et l'efface avec le compte.

## Conséquences

- ➕ Un développeur Java qui découvre Go saute en cinq minutes les bases qu'il connaît, et le Tour s'ouvre là où il a
  quelque chose à apprendre.
- ➖ **Un domaine réussi par chance** saute des notions qu'on ne connaît pas, une fois sur neuf avec trois choix : leurs
  cartes viennent quand même, plus tard dans la révision, et leurs pages restent ouvertes dans le Tour. Annuler le test
  répare une surestimation.
- ➕ La maîtrise et l'indice de préparation restent honnêtes : une notion connue n'est pas acquise tant que ses cartes
  ne sont pas réussies.
- ➖ La file de révision, la carte du parcours, le sommaire du Tour et le compte à rebours lisent les notions connues :
  une requête de plus chacun, sur un index (`user_id`, `subject_slug`).
- ➖ Le tirage lit les cartes du sujet que le serveur corrige sans exécuter de code (quelques milliers de lignes légères,
  comme le tirage du défi du jour), à chaque test et à chaque domaine corrigé : le test n'est demandé qu'au premier
  login et depuis le Tour.
- 🔁 Le test ne regarde que les notions débutantes. Un test des notions intermédiaires, pour qui prépare un entretien,
  pourrait suivre le même modèle si la mesure du parcours (ADR 0064) montre qu'on en a besoin.
