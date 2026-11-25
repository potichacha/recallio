# 0076. La page « Bêta » des admins : ce que font les testeurs, en agrégé, sans donnée nouvelle ni outil tiers

- Statut : accepté
- Date : 2026-11-25
- Prolonge : ADR 0001 (monolithe modulaire), ADR 0050 (la rétention), ADR 0064 (le parcours mesuré), ADR 0056 (le port
  `PersonalData`, dont celui-ci reprend le patron), ADR 0070 (aucune donnée personnelle dans les logs), ADR 0074 (la
  politique de confidentialité dit ce que le code garde), ADR 0042 (les signalements)
- Origine : le déploiement (phase 1, étape 7), « la page "Bêta" des admins », décidée le 2026-11-24 ; cet ADR couvre le
  backend, l'écran vient dans la PR suivante

## Contexte

La bêta ouvre pour quelques testeurs. La rétention (ADR 0050) dit combien reviennent, le parcours (ADR 0064) où l'on
perd les visiteurs. Rien ne dit ce que font ceux qui restent : quels sujets ils révisent, quelles cartes ils ratent,
où ils s'arrêtent dans le Tour, quelles fonctionnalités ne servent à personne. Ce sont les questions qui décident de
la suite de la feuille de route.

Les réponses sont déjà en base : chaque révision, chaque notion validée, chaque duel a sa ligne. Quatre contraintes :

- **le RGPD et la section 4.5 de `CLAUDE.md`** : une mesure ne doit pas devenir un suivi des personnes, et la politique
  de confidentialité (ADR 0074) doit rester exacte ;
- **la frontière des modules** (ADR 0001) : ces lignes sont dans une vingtaine de tables de huit modules, et un module
  ne lit pas les tables d'un autre ;
- **le journal de révision ne connaît que l'identifiant des cartes** : leur sujet, leur notion et leur question sont
  au module `content` ;
- **YAGNI** : dix testeurs ne justifient ni un entrepôt de données, ni des filtres.

## Options envisagées

**L'outil** :

- **Un outil tiers** (Metabase, Grafana sur la base, PostHog) : des graphes tout faits, mais un service de plus à
  héberger et à sécuriser, un accès direct à toutes les tables, et, pour un outil hébergé ailleurs, un sous-traitant de
  plus dans la politique de confidentialité.
- **Des événements de mesure** (une table `usage_event` alimentée par chaque fonctionnalité) : une seule table à lire,
  mais une donnée de plus à garder, à exporter et à effacer, pour redire ce que les tables métier disent déjà.
- **Lire ce qui est déjà gardé** (retenu) : aucune table, aucune colonne, aucun événement de plus.

**Comment atteindre les tables de chaque module** :

- **Un service qui lit tout en SQL**, dans `gamification` : le plus court, et la fin des frontières ; le test
  d'architecture ne voit pas le SQL, personne ne le verrait.
- **Une façade publique de plus par module** (`review.ReviewStatistics`, `user.AccountStatistics`...) : conforme à
  l'ADR 0001, mais huit interfaces pour une seule page, et `gamification` dépendrait de modules qu'il ne connaît pas
  aujourd'hui (`feedback`, `live`, `notification`, lequel dépend déjà de lui : un cycle).
- **Un port dans `platform`, que chaque module implémente** (retenu) : le patron de `PersonalData` (ADR 0056). Le
  rapport ne dépend que du port.

**La forme du port** :

- **Une seule question générique** (« des mesures nommées, par clé ») : un module de plus n'y changerait rien, mais
  tout y passe en chaînes de caractères, et le compilateur ne vérifie plus rien.
- **Quelques questions typées, chacune avec une réponse vide par défaut** (retenu) : un module ne répond qu'à ce qui le
  concerne, et lit ce qu'on attend de lui dans la signature.

**Le sujet et la question d'une carte ratée** (le journal est à `review`, la carte à `content`) :

- **Une jointure SQL entre `review_log` et `card`** : une requête d'un module sur la table d'un autre.
- **Charger le journal et compter en Java** : tout `review_log` en mémoire.
- **Chacun sa moitié** (retenu) : `review` compte par carte dans son journal, en base ; le sujet, la notion et la
  question viennent ensuite des façades existantes de `content` (`CardDirectory.subjectsOf`, `currentVersions`), pour
  les seules cartes retenues.

**Les comptes actifs**, dénominateur de l'usage des fonctionnalités :

- **Ceux qui ont révisé** : simple, mais qui apprend commence par le Tour (ADR 0059) sans réviser, et le défi du jour
  n'est pas une révision (ADR 0067). Une fonctionnalité pourrait dépasser 100 %.
- **Ceux qui ont utilisé au moins une fonctionnalité datée sur la fenêtre** (retenu) : il faut réunir des comptes venus
  de plusieurs modules sans compter deux fois le même, donc les identifier le temps du calcul.

## Décision

1. **Aucune donnée nouvelle.** Le rapport lit les tables existantes. La seule migration (V34) ajoute un index,
   `review_log (reviewed_at)` : l'index existant commence par `user_id` et ne sert pas une lecture de trente jours tous
   utilisateurs confondus, sur la seule table qui grossit d'une ligne par réponse. Les autres tables lues par date
   (`tour_progress`, `mock_exam`, `duel_player`...) ont une ligne par notion, par examen ou par duel : elles restent
   sans index de plus tant qu'une mesure ne le demande pas.

2. **Agrégé, jamais une personne.** La réponse ne contient ni identifiant d'utilisateur, ni nom, ni adresse : des
   comptes et des parts. Le contenu, lui, est nommé (un sujet, une notion, le début d'une question) : ce n'est pas une
   personne. Un test relit le JSON et y cherche les identifiants et le nom des comptes qu'il a semés.

3. **Le port `platform.domain.UsageStatistics`**, sur le patron de `PersonalData`. Chaque module qui a de quoi
   répondre l'implémente dans son `infrastructure` (`ReviewUsageStatistics`, `UserUsageStatistics`...), en SQL agrégé
   (`GROUP BY`, `LIMIT`), sur ses seules tables. Sept questions, toutes vides par défaut :

   | Question | Qui répond |
   |---|---|
   | `featureUsers` : les comptes de chaque fonctionnalité | `review`, `gamification`, `ranked`, `live`, `group`, `notification`, `user` |
   | `dailyCounts` : des comptes par jour | `gamification` (révisions), `user` (comptes créés), `review` (Tour) |
   | `subjectCounts` : des comptes par sujet | `user` (suivis), `review` (révisions) |
   | `mostFailedCards` : les cartes au plus fort taux d'échec | `review` |
   | `openReports`, `mostReportedCards` : les signalements ouverts par carte | `feedback` |
   | `tourStops` : les comptes par notion de leur dernière page du Tour | `review` |

   `gamification.application.BetaReportService` rassemble les réponses, sans connaître une table, et nomme le contenu
   par `content.CardDirectory` et `content.SubjectDirectory`. Les parts et les taux se calculent dans
   `gamification.domain.beta`, en Java pur.

4. **Les identifiants restent dans le backend.** `featureUsers` rend des ensembles d'identifiants : c'est ce qui permet
   de réunir les comptes de plusieurs modules sans compter deux fois le même. Ils servent au calcul et n'en sortent
   pas.

5. **Le sujet d'une réponse** : `review` demande à `content` le sujet des cartes révisées sur la fenêtre
   (`CardDirectory.subjectsOf`), puis rend cette correspondance à la base (`unnest`), qui compte par sujet les
   réponses, les réponses justes et les comptes distincts. Aucune requête ne joint les tables de deux modules.

6. **Un seul endpoint**, `GET /api/v1/admin/beta`, rôle admin, sans paramètre. La fenêtre est fixe : les trente
   derniers jours du calendrier UTC, aujourd'hui compris (`BetaReport.DAYS`). La rétention et le parcours gardent leurs
   endpoints : l'écran les appellera à côté.

7. **Le contenu du rapport** :
   - **par jour** : les comptes qui ont révisé, les cartes révisées et les objectifs atteints (`daily_activity`, au jour
     de chacun dans son fuseau) ; les comptes créés et les notions validées dans le Tour (au jour UTC) ;
   - **par sujet** : les comptes qui le suivent (à ce jour), ceux qui l'ont révisé, les réponses et la part de réponses
     justes. Tous les sujets publiés y figurent, et tout autre sujet suivi ou révisé ; le plus révisé d'abord ;
   - **les 20 cartes les plus ratées** parmi celles qui ont reçu au moins 5 réponses (`MIN_ANSWERS`) dont une fausse,
     avec leur sujet, leur notion, le début de leur question, leur format et leurs signalements ouverts ;
   - **les 10 cartes les plus signalées** : `/api/v1/admin/content-reports` montre les signalements un par un, pour
     les traiter ; cette liste montre où ils s'accumulent ;
   - **le Tour** : les comptes qui ont validé une notion sur la fenêtre, et, par sujet, le nombre de comptes dont la
     dernière page ouverte est dans chaque chapitre ;
   - **les fonctionnalités** (`UsageFeature`) : pour chacune, les comptes actifs qui l'ont utilisée et leur part des
     comptes actifs.

8. **Daté, ou à ce jour.** Dix fonctionnalités gardent l'instant de chaque usage et se bornent à la fenêtre : révision,
   Tour, exercice de code, examen blanc, défi du jour, duel classé, partie live, défi de groupe, quête, test de niveau.
   Quatre sont des états sans histoire : le compte à rebours (`interview_plan`), les rappels (`push_subscription`), le
   profil public et l'appartenance à un groupe. Pour celles-là, on compte les comptes **actifs sur la fenêtre** qui les
   ont **à ce jour** ; le champ `dated` de la réponse le dit. Une part ne dépasse donc jamais 100 %.

9. **Une part de rien est absente, pas nulle** : le taux de réussite d'un sujet sans réponse, la part d'une
   fonctionnalité quand aucun compte n'est actif.

10. **Rien à exporter ni à effacer de plus** (ADR 0056) : aucune table. La politique de confidentialité (ADR 0074) ne
    change pas de contenu ; voir la dernière conséquence.

## Ce qui a été écarté, faute de donnée exacte

- **Les pages du Tour ouvertes par jour** : seule la dernière page de chacun par sujet est gardée (`tour_last_page`).
  On sait où chacun en est, pas ce qu'il a lu ni quand.
- **L'abandon du Tour** : « s'est arrêté au chapitre 2 » ne distingue pas celui qui a abandonné de celui qui reprendra
  demain. La liste montre une position, pas un abandon.
- **Le taux de réussite par l'XP** : le registre de l'XP a le sujet de chaque révision et une entrée par réussite
  vérifiée, mais il dépend du barème (une XP à zéro n'écrit rien). Le journal de révision fait foi.
- **L'entraînement libre, les exécutions du Tour, les « À toi »** : rien n'en est enregistré (ADR 0038, 0037). Un
  exercice « rendu » est donc un exercice rendu en révision.
- **Le test de niveau raté partout** : il ne laisse aucune ligne. La fonctionnalité compte ceux dont au moins un
  domaine est réussi.
- **Les rappels reçus ou ouverts, les profils publics consultés** : on sait qui a accepté et qui a publié, pas ce qui
  a été lu.
- **Une série par semaine** : les huit semaines du parcours restent au parcours ; rien de ce rapport ne se lit mieux à
  la semaine qu'au jour.

## Conséquences

- ➕ Le propriétaire voit l'usage réel sans ouvrir la base, sans outil tiers, sans bandeau de consentement, et sans rien
  garder de plus.
- ➕ Les frontières tiennent : le test d'architecture reste vert sans exception, et un module qui ajoute une
  fonctionnalité l'ajoute à `UsageFeature` et à son propre `UsageStatistics`.
- ➕ Le journal de révision n'est jamais chargé : chaque chiffre est compté en base.
- ➖ **Avec dix testeurs, un agrégat peut laisser deviner quelqu'un** : « un compte a passé un examen blanc » désigne
  presque une personne pour qui connaît les testeurs. La page est réservée aux admins, qui ont déjà accès à la base, et
  rien n'en sort (ni export, ni partage). Un seuil d'anonymat n'aurait pas de sens à cette échelle ; il en aura une
  avant d'ouvrir ces chiffres à quelqu'un d'autre (les statistiques des organisations, phase 4).
- ➖ Les ensembles d'identifiants de `featureUsers` tiennent en mémoire : un par compte et par fonctionnalité. C'est
  négligeable pour une bêta, et pour quelques dizaines de milliers de comptes ; au-delà, l'union devra se faire
  en base.
- ➖ Le port a sept questions, dont quatre n'ont qu'un module pour y répondre. C'est le prix d'un seul point de passage
  entre les modules et le rapport ; une façade par module aurait coûté un cycle.
- ➖ Deux calendriers dans la même ligne du jour : le jour de chacun pour la révision (`daily_activity` ne garde que
  lui), le jour UTC pour le reste. Au bord de la fenêtre, un jour local peut déborder d'un jour, comme pour la
  rétention (ADR 0050). La tendance n'en dépend pas.
- ➖ Ce que les comptes supprimés ont joué à plusieurs reste, détaché d'eux (ADR 0056) : un duel, un défi de groupe ou
  une partie live d'un compte supprimé dans la fenêtre compte encore un compte actif. Les comptes créés, eux, se
  comptent dans `app_user` : un compte supprimé n'y est plus.
- ➖ Une carte retirée, ou d'un sujet dépublié, n'a plus que son identifiant et son sujet dans les listes : les façades
  de `content` ne montrent que le contenu publié. Les exercices rendus se reconnaissent de même aux exercices encore
  actifs.
- ➖ Le rapport fait une trentaine de requêtes, et relit les notions des sujets publiés pour ranger les arrêts du Tour
  par chapitre. C'est une page d'admin, ouverte quelques fois par jour : pas de cache.
- 🔁 **La politique de confidentialité** dit pourquoi chaque donnée est gardée (« pour te reposer chaque carte au bon
  moment »...). Elle ne dit pas que l'éditeur en tire des chiffres d'ensemble pour améliorer le service ; la base
  légale « intérêt légitime » ne cite que les comptes de visites et d'essais. Aucune phrase n'interdit cet usage, et
  aucune donnée n'est ajoutée, mais une ligne de plus la rendrait exacte : à décider par le propriétaire avant la mise
  en service.
  - **Note du 2026-11-25** : c'est fait, avec l'écran (`/admin/beta`). La politique le dit à deux endroits : la ligne
    « Ta progression » du tableau, et l'intérêt légitime parmi les bases légales (« les chiffres d'ensemble tirés de
    l'activité des comptes [...], sans fiche par personne »). Sa date passe au 25 novembre 2026.
