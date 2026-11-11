# 0056. Supprimer son compte et récupérer ses données

- Statut : accepté
- Date : 2026-11-11
- Prolonge : ADR 0001 (monolithe modulaire), ADR 0043 (profil public), CLAUDE.md section 4.5 (sécurité)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 17

## Contexte

Le RGPD donne à chacun le droit de récupérer ses données (article 20) et de les faire effacer (article 17). Rien ne
le permettait : il fallait le demander à l'admin, qui n'avait lui-même aucun outil.

Les données d'un utilisateur sont réparties dans plus de vingt tables de 8 modules. Une seule a une clé étrangère vers
`app_user` : les modules ne se connaissent que par leur API publique (ADR 0001), pas par leurs tables. Le module
`user`, dont tous dépendent, ne peut donc pas effacer leurs données lui-même sans créer un cycle.

L'identité, elle, est chez le fournisseur OIDC : le backend valide des jetons et ne sait rien d'autre de lui.

## Options envisagées

**Comment atteindre les données de chaque module** :

- **Des événements CDI** (`AccountDeleted`), comme pour l'XP : bien pour effacer, mais un événement ne rend rien, et
  l'export a besoin d'une réponse.
- **Des clés étrangères `ON DELETE CASCADE`** vers `app_user` : la base ferait le travail, mais les tables d'un module
  dépendraient de celles d'un autre, et rien ne dirait ce qu'on garde.
- **Un port, `PersonalData`** (retenu) : chaque module l'implémente pour exporter et pour effacer.

**Quand effacer** :

- **Après un délai de 30 jours**, pour laisser changer d'avis : il faut un état « en cours de suppression » dans chaque
  écran et une tâche planifiée.
- **Tout de suite** (retenu) : plus simple, et c'est ce que la demande dit. L'export, proposé juste au-dessus, est le
  filet.

**Ce que deviennent les données partagées** (un duel, un défi de groupe, une partie live) :

- **Les effacer aussi** : l'historique de l'adversaire et le classement du groupe changeraient après coup.
- **Les garder, détachées de la personne** (retenu).

**L'identité chez le fournisseur OIDC** :

- **Le backend l'efface par l'API d'administration** : il lui faudrait un secret d'administration du fournisseur, et
  du code propre à Keycloak.
- **L'admin l'efface à la main**, sur demande : rien d'automatique, et un délai.
- **Le front y envoie l'utilisateur** (retenu) : Keycloak sait faire confirmer et effacer un compte à la demande d'une
  application (`kc_action=delete_account`).

## Décision

**Le port** `com.recallio.platform.domain.PersonalData` : `section()`, `export(userId)`, `erase(userId)`. Il est dans
`platform`, que tous les modules utilisent. Chaque module qui garde des données en a un adaptateur dans son
`infrastructure` (`ReviewPersonalData`, `GroupPersonalData`...), en SQL, comme le reste de la persistance.
`AccountService`, dans `user`, les appelle tous dans une seule transaction.

**Deux routes**, pour l'utilisateur connecté seulement, sans identifiant dans l'adresse :

- `GET /api/v1/me/account/export` : un fichier JSON, une section par module, une liste de lignes par table. Les lignes
  sont celles de la base, telles quelles : un export doit tout contenir, y compris ce qu'on ajoutera demain. Seules
  les clés de chiffrement des abonnements aux rappels n'en sortent pas. C'est la seule route sans pagination.
- `DELETE /api/v1/me/account` : efface, et répond 204.

**Ce que la suppression fait** :

| Données | Sort |
|---|---|
| Mémoire des cartes, journal de révision, examens blancs, Tour | effacés |
| XP, activité quotidienne, quêtes, succès | effacés |
| Profil, sujets suivis, profil public, abonnements aux rappels | effacés |
| Cote, insignes, place dans la file du classé ; duel ouvert que personne n'a rejoint | effacés |
| Appartenances aux groupes | effacées ; le rôle de créateur passe au membre le plus ancien, un groupe vide est supprimé |
| Duels joués, réponses aux défis, résultats des parties live | gardés, sous le nom « Compte supprimé » |
| Défis lancés, signalements d'erreurs du contenu | gardés |

Ce qui est gardé porte encore l'identifiant applicatif de l'utilisateur, un UUID. Une fois le profil effacé, il ne
mène plus à un nom, à une adresse ni à une identité : la donnée est anonyme.

**L'identité** : après la réponse 204, le front oublie le jeton et renvoie vers le fournisseur avec
`kc_action=delete_account`. Keycloak fait confirmer (et redemande le mot de passe si la connexion est ancienne), efface
l'identité, et propose de revenir à l'application. Le nom de l'action est une variable du front
(`VITE_OIDC_DELETE_IDENTITY_ACTION`) : vide, l'utilisateur est seulement déconnecté.

**Dans la PWA** : la section « Mes données » du profil. La suppression demande de retaper son nom affiché.

**Le garde-fou** : `AccountResourceTest` liste les tables qui ont une colonne `user_id`. Une nouvelle table fait
échouer le test tant qu'elle n'est rangée ni parmi les effacées ni parmi les gardées, donc tant que son module ne l'a
pas ajoutée à son `PersonalData`.

## Conséquences

- Un nouveau module qui garde des données personnelles implémente `PersonalData` ; le test le rappelle.
- La suppression ne se rattrape pas. Une sauvegarde de la base contient encore le compte jusqu'à son expiration : à
  dire dans la politique de confidentialité, avec la durée de conservation des sauvegardes (déploiement).
- Si l'utilisateur annule chez Keycloak, son identité reste : en se reconnectant, il retrouve un profil neuf. L'écran
  de confirmation le dit.
- La règle « le plus ancien membre devient créateur » est écrite en SQL dans `GroupPersonalData`, et non dans le
  domaine du module `group` : c'est le seul endroit où elle sert. Si un créateur peut un jour céder son groupe, elle
  remontera dans le domaine.
- L'export se construit en mémoire. Un journal de révision de dix ans tient en quelques mégaoctets ; au-delà, il
  faudra l'écrire au fil de l'eau.
- Les journaux notent qu'un compte a été supprimé, sans dire lequel.
- **Au déploiement** : activer l'action « Delete Account » dans le royaume Keycloak de production et donner le rôle
  `delete-account` du client `account` aux utilisateurs (par le rôle par défaut du royaume), comme dans
  `infra/keycloak/recallio-realm.json` ; renseigner l'adresse de base du client pour le lien de retour.
