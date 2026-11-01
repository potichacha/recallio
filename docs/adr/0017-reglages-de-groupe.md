# 0017. Réglages de groupe : ce que le groupe révise, et la révision avec le groupe

- Statut : accepté
- Date : 2026-11-01

## Contexte

Le créateur d'un groupe définit ce que ses membres révisent ensemble et comment le classement les compare
(CLAUDE.md, section 6.4) : sujets et thèmes inclus, types de cartes, objectif quotidien, visibilité du classement. Les
membres doivent ensuite pouvoir « réviser avec le groupe ». Le classement lui-même viendra à l'étape suivante : ses
réglages sont posés dès maintenant pour qu'il n'ait pas à modifier le modèle.

## Options envisagées

Pour choisir des thèmes :

1. **Une liste de thèmes qui restreint leur sujet** (retenu) : un sujet dont aucun thème n'est coché les garde tous. Le
   cas courant (« tout Java ») ne demande aucun thème, et un thème ajouté plus tard au document y entre de lui-même.
2. **La liste exhaustive des thèmes** : explicite, mais un nouveau chapitre resterait hors du groupe tant que le créateur
   ne l'a pas coché.

Pour « aucun format coché » : **tous les formats** (retenu), y compris ceux à venir (les QCM de l'étape 5), plutôt
qu'une liste figée au moment du réglage.

Pour le stockage :

1. **Une table `group_settings`** (retenu), une ligne par groupe, avec des tableaux PostgreSQL (`text[]`) pour les sujets,
   les thèmes et les formats : de petits ensembles, toujours lus avec le groupe, jamais interrogés seuls.
2. Des tables d'association (`group_subject`, `group_theme`) : plus relationnel, mais trois tables et trois requêtes pour
   un formulaire.

## Décision

- Le domaine `GroupSettings` porte les règles : sujets et thèmes sans doublon, au plus 50 sujets et 500 thèmes, objectif
  quotidien entre 1 et 200 cartes. Les réglages par défaut d'un groupe neuf, sans ligne en base : **aucun sujet**, tous
  les formats, objectif de **10 cartes** (comme la révision personnelle), classement **nominatif** (l'invité est prévenu
  en rejoignant que les membres verront son nom).
- Le créateur seul les remplace : `PUT /api/v1/groups/{id}/settings` (403 pour un membre). Le serveur refuse un sujet non
  publié (`group.unknown-subject`) et un thème qui n'appartient à aucun sujet choisi (`group.unknown-theme`), tous deux
  en 422. Il obtient les thèmes d'un sujet par `content.SubjectDirectory.themeSlugs`.
- Tous les membres voient les réglages (`settings` dans le groupe).
- **Réviser avec le groupe** : `GET /api/v1/review-queue?group={id}`. Le module review lit le périmètre par l'API publique
  `group.GroupReviewScopes` (404 pour un non-membre), et le traduit en filtre de cartes. Un sujet ou un format demandé en
  plus restreint encore la file. Le quota de nouvelles cartes reste celui de l'utilisateur, tous contextes confondus : un
  groupe ne permet pas de dépasser sa limite du jour.
- Le filtre de cartes du module content (`CardFilter`) accepte plusieurs formats et une sélection de thèmes au sein des
  sujets, traduite en une seule requête.
- Les dépendances entre modules : `review` → `group` → `content`, `user`. Aucun cycle.

## Conséquences

- Sujets et thèmes sont désignés par leur slug, sans clé étrangère (ADR 0001). Un sujet repassé en brouillon sort de la
  révision du groupe sans erreur. Un thème dont le titre change change de slug : il sort de la sélection, et son sujet
  retrouve tous ses thèmes si c'était le seul choisi. Le créateur le voit dans ses réglages et peut le cocher de nouveau.
- L'objectif quotidien et la visibilité du classement n'ont pas encore d'effet : ils servent au classement hebdomadaire
  (étape 4).
- La révision avec un groupe ne touche pas aux sujets suivis : elle s'ajoute à la révision personnelle, avec la même
  progression par carte.
