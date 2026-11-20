# 0066. Le compte à rebours avant l'entretien : une date par sujet suivi, un plan par semaine

- Statut : accepté
- Date : 2026-11-20
- Prolonge : ADR 0032 (les parcours), ADR 0034 (la maîtrise par notion), ADR 0041 (l'indice de préparation), ADR 0056
  (les données personnelles)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 25

## Contexte

Quelqu'un qui a un entretien Java le 15 décembre veut savoir combien de jours il lui reste, quelles notions travailler
cette semaine, et s'il est en avance ou en retard. L'indice de préparation (ADR 0041) dit déjà s'il est **prêt** : la
part de ses notions essentielles (priorité 1) acquises, puis maîtrisées. Il ne dit pas **comment y arriver à temps**.

Ce qui existe déjà :

- **la maîtrise des notions** (ADR 0034) est calculée à la demande, dans le module `review`, sans rien stocker ; elle
  n'a pas d'historique ;
- **le parcours « préparer un entretien »** (ADR 0032) sert les nouvelles cartes par priorité, les notions essentielles
  d'abord (`NewCardOrder`) ;
- **une notion acquise ne se perd pas** (ADR 0034) : oublier ses cartes fait baisser l'indice, pas l'acquisition.

## Options envisagées

**Où vit la date** :

- **Dans `followed_subject`, du module `user`**, à côté du parcours : mais la ligne de départ du plan est une donnée de
  la maîtrise, que seul `review` calcule. Le module `user` devrait appeler `review`, qui dépend déjà de lui : un cycle.
- **Une table du module `review`** (retenu), à côté de la maîtrise et de l'indice.

**Une date pour tout, ou par sujet** :

- **Une date pour tout** : un entretien porte souvent sur plusieurs sujets, mais l'indice est par sujet, et un sujet
  suivi pour le plaisir n'a pas à entrer dans le plan.
- **Une date par sujet suivi** (retenu), comme le parcours : on prépare Java et Spring Boot pour le même entretien en
  donnant la même date aux deux.

**L'avance et le retard** :

- **D'après l'historique de la maîtrise** : elle n'est pas stockée, et la recalculer dans le passé (le journal rejoué
  jusqu'à chaque date) est coûteux.
- **D'après une ligne de départ et un objectif par semaine** (retenu) : le nombre de notions à acquérir le jour où la
  date est fixée, puis un objectif cumulé par semaine, comparé à ce qui reste aujourd'hui.

**Le plan** :

- **Figé** : la liste des notions de chaque semaine, stockée le jour où la date est fixée. Il faudrait la tenir à jour
  quand une notion est acquise plus tôt que prévu, ou quand le contenu change.
- **Recalculé** (retenu) : seul le nombre de départ est stocké ; les notions de la semaine suivent ce qu'on a acquis.

## Décision

1. **La table `interview_plan`** (migration V31), clé (`user_id`, `subject_slug`) : `interview_on` (le jour de
   l'entretien), `planned_on` (le jour local où la date a été fixée) et `notions_to_acquire` (la ligne de départ).
   Rien d'autre n'est stocké. Comme les autres tables du module, pas de clé étrangère vers celles des autres modules.
2. **La date** :
   - elle est optionnelle, et se donne à un sujet suivi qui a des notions essentielles (un sujet à référentiel) ;
   - elle doit être **après aujourd'hui**, dans le fuseau de l'utilisateur, et **au plus un an** plus tard ;
   - la changer **repart de zéro** : `planned_on` devient aujourd'hui, la ligne de départ est recomptée ;
   - arrêter de suivre le sujet la **garde**, sans la montrer : elle revient si on le suit de nouveau, comme la
     progression (`CLAUDE.md`, section 6.1).
3. **Le plan**, calculé dans le domaine (`review.domain.interview.InterviewCountdown`), en Java pur, à partir de la
   maîtrise déjà calculée pour le tableau de bord, avec le jour de l'utilisateur (`Clock` injectée, son fuseau) :
   - **les notions du plan** : les notions essentielles du sujet pas encore acquises, dans l'ordre où le parcours
     « préparer un entretien » sert leurs cartes (`NewCardOrder.notionOrder`, le même ordre que les cartes) ;
   - **la ligne de départ** `S0` : leur nombre le jour où la date est fixée ;
   - **les semaines** comptent depuis `planned_on` (la semaine 1 est ce jour-là et les six suivants), pas celles du
     calendrier ; il y en a `ceil(jours jusqu'à l'entretien / 7)`, la dernière s'arrêtant la veille de l'entretien ;
   - **la dernière semaine sert à réviser** quand il y en a au moins deux : les notions sont réparties sur `W` semaines,
     toutes sauf la dernière (`W = 1` pour un plan d'une semaine) ;
   - **l'objectif cumulé** à la fin de la semaine `k` : `T(k) = ceil(S0 × min(k, W) / W)` notions acquises ;
   - **le fait** : `max(0, S0 − restantes aujourd'hui)` ;
   - **cette semaine** : `max(0, T(k) − fait)` notions, les premières du plan ; pendant la semaine de révision, c'est ce
     qu'il reste à rattraper ;
   - **les semaines suivantes** : leurs dates, leur nombre de notions (`T(j) − T(j − 1)`), la semaine de révision
     marquée.
4. **Le statut**, le premier qui s'applique : `PASSED` (le lendemain de l'entretien et après), `READY` (l'indice dit
   « prêt »), `BEHIND` (une semaine passée n'est pas finie : `fait < T(k − 1)`), `AHEAD` (l'objectif de la semaine est
   atteint et il reste des notions), sinon `ON_TRACK`. Le jour de l'entretien, il reste 0 jour et le statut se calcule
   normalement.
5. **L'API**, pour l'utilisateur connecté seulement :
   - `GET /api/v1/me/interviews` : les comptes à rebours des sujets suivis qui ont une date, dans l'ordre du catalogue ;
   - `PUT /api/v1/me/interviews/{sujet}`, corps `{ "date": "2026-12-15" }` : fixe ou change la date, répond le compte à
     rebours ; idempotente par nature ;
   - `DELETE /api/v1/me/interviews/{sujet}` : 204, qu'une date ait existé ou non ;
   - les refus répondent 422 (Problem Details), comme les cas voisins : `user.subject-not-followed` pour le parcours
     d'un sujet non suivi, `review.not-enough-exam-questions` pour un examen blanc impossible. Leurs codes :
     `interview.invalid-date`, `interview.subject-not-followed`, `interview.no-essential-notions`.
6. **La PWA** : sous la jauge « Prêt pour l'entretien » de chaque sujet du tableau de bord, une action discrète pour
   donner une date, puis le compte à rebours : les jours qui restent, le statut (une icône et un mot, jamais la couleur
   seule), les notions de la semaine (chacune mène à sa leçon), les semaines suivantes repliées, « Changer la date » et
   « Retirer la date ». Si le parcours du sujet n'est pas « préparer un entretien », une phrase le dit, avec un lien
   vers les parcours du profil. Au premier login, qui prépare un entretien peut donner la date, facultative ; une date
   refusée ne bloque pas l'arrivée.
7. **Les données personnelles** (ADR 0056) : `ReviewPersonalData` exporte les lignes de `interview_plan` et les efface
   avec le compte.

## Conséquences

- ➕ Le plan dit quoi faire cette semaine, et pas seulement où l'on en est : l'indice mesure, le compte à rebours
  guide.
- ➕ Aucune requête de plus que celles du tableau de bord, et trois valeurs stockées par sujet : changer une règle du
  plan ne demande aucune reprise de données.
- ➖ **Le plan vise l'acquisition, pas la maîtrise** : une notion maîtrisée demande des semaines de révisions espacées
  (ADR 0034), qu'aucun plan ne peut comprimer. D'où la semaine de révision, et le statut « dans les temps » quand tout
  est acquis mais que l'indice n'a pas atteint 80 % : la maîtrise viendra des révisions.
- ➖ **Ce qui est fait se mesure sur ce qui reste** : une notion qui repasse sous « acquise » fait reculer le plan,
  par exemple une notion ajoutée au référentiel, ou une petite notion qui reçoit de nouvelles cartes et en demande donc
  davantage. C'est voulu : le plan suit le contenu du jour. Une notion **oubliée**, elle, reste acquise (ADR 0034) :
  l'oubli fait baisser l'indice, pas l'avancement du plan.
- ➖ **Changer la date repart de zéro** : la ligne de départ est recomptée, et l'avance ou le retard d'avant est
  oublié. Il n'y a qu'un plan par sujet, pas d'historique.
- 🔁 Le plan sert ses notions dans l'ordre du parcours « préparer un entretien », même si l'utilisateur en suit un
  autre ; la PWA le signale, puisque c'est ce parcours qui sert d'abord les notions essentielles en révision.
