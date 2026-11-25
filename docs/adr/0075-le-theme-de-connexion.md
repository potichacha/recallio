# 0075. Le thème de connexion : les pages de Keycloak à l'allure de Recallio, par une feuille de style, des messages et un seul gabarit

- Statut : accepté
- Date : 2026-11-25
- Prolonge : ADR 0058 (le design system « Rico »), ADR 0069 (Keycloak hébergé, sous `auth.<domaine>`), ADR 0071 (le
  serveur de l'application, le royaume de production), ADR 0074 (les pages légales, qui laissait à cette étape le lien
  depuis la page d'inscription), ADR 0054 (aucune origine tierce), ADR 0056 (la suppression du compte, confirmée chez
  Keycloak)
- Origine : le déploiement (phase 1, étape 7), « le thème de la page de connexion », décidé le 2026-11-24

## Contexte

Se connecter, s'inscrire, retrouver son mot de passe, vérifier son adresse, confirmer la suppression de son compte : ces
pages ne sont pas dans la PWA. Keycloak les sert, sur sa propre origine, avec son thème par défaut : un formulaire gris
et carré, sur une image de fond sombre, qui vouvoie (« Connectez-vous à votre compte », « Veuillez renseigner une
valeur ») et qui, en local, parlait anglais. Un visiteur quitte donc une application violette et ronde, qui le tutoie,
au moment précis où il décide de créer un compte.

L'ADR 0074 a aussi laissé un manque : la page d'inscription ne renvoie pas aux mentions légales ni à la politique de
confidentialité.

Les contraintes :

- **Dependabot met Keycloak à jour tout seul** (`keycloak:26.8` vers `26.9`…) : tout ce que le thème recopie du thème
  par défaut vieillit sans bruit ;
- **aucune requête vers un tiers** (ADR 0054) : la police se sert d'ici ;
- **le dépôt est public, et une seule configuration vaut pour tous les environnements** : l'adresse du site ne s'écrit
  pas dans le thème ;
- **pas d'image Keycloak à construire ni à publier** : le compose de production n'utilise que l'image officielle
  (ADR 0071) ;
- **la page est sur une autre origine que l'application** : elle ne lit ni sa feuille de style, ni son stockage.

## Ce que le thème par défaut de Keycloak 26.8 permet

Lu dans l'image (`org.keycloak.keycloak-themes-26.8.2.jar`), pas de mémoire :

- le thème de connexion par défaut s'appelle `keycloak.v2`, sur PatternFly 5 ; il hérite de `base`. Ses gabarits posent
  des classes CSS qu'ils lisent dans `theme.properties` ;
- un thème enfant hérite de tout (`parent=keycloak.v2`) et ne redonne que ce qu'il change : des propriétés, des
  messages, des ressources, des gabarits ;
- **`styles`** liste les feuilles de style ; la propriété de l'enfant remplace celle du parent, il faut donc y garder
  `css/styles.css` ;
- **les messages** (`messages/messages_fr.properties`, en UTF-8) se surchargent clé par clé ;
- **`footer.ftl`** est un point d'extension prévu : `template.ftl` l'importe et appelle sa macro `content` au bas de la
  carte, et le thème parent la laisse vide (« You can override this file in your custom theme to declare a custom login
  footer element ») ;
- **une propriété du thème peut lire l'environnement** : `${env.NOM}` dans `theme.properties`, puis
  `${properties.nom}` dans un gabarit ;
- **le thème sombre** est déjà là : un script du gabarit pose la classe `pf-v5-theme-dark` sur la page quand le système
  le demande (`darkMode=true`) ;
- **l'icône de l'onglet** se nomme par `favIcon` et `favIconType` ;
- **aucun emplacement n'est prévu dans le formulaire d'inscription** pour une phrase : le seul est l'acceptation de
  conditions (`termsAcceptance`), qui vient avec sa case à cocher.

## Options envisagées

**Comment changer l'allure** :

- **Recopier les gabarits** (`template.ftl`, `login.ftl`, `register.ftl`…) et y écrire notre HTML : une liberté
  totale, et une dette à chaque version. `template.ftl` porte des scripts de Keycloak (la session suivie entre onglets,
  les clés d'accès) : une copie figée casserait en silence à une montée de version.
- **Un outil de construction de thèmes** (Keycloakify) : des pages en React, mais une dépendance, une étape de build et
  un jar à livrer, pour une dizaine de pages.
- **Une feuille de style, des messages et des propriétés, sur le thème parent** (retenu) : les gabarits restent ceux de
  Keycloak, à jour avec lui.

**Où mettre les liens légaux** :

- **Recopier `register.ftl`** pour écrire la phrase au-dessus du bouton : un gabarit de page figé.
- **L'acceptation de conditions de Keycloak** : une case à cocher, que l'ADR 0074 écarte (la politique informe, elle ne
  demande pas un consentement).
- **Le pied de page prévu, `footer.ftl`** (retenu) : sur la page d'inscription, il écrit la phrase ; sur les autres, les
  deux liens.

**Comment livrer le thème** :

- **Une image Keycloak maison**, avec le thème dedans : à construire, publier et suivre à chaque version.
- **Un jar dans `providers/`** : une étape de build.
- **Un dossier du dépôt, monté en lecture seule** (retenu), comme le fichier du royaume.

**Le thème sombre** :

- **Le choix fait dans le profil** : impossible, il vit dans le stockage d'une autre origine.
- **Le réglage du système** (retenu), ce que fait déjà le thème parent.

## Décision

### 1. Un thème `recallio`, enfant de `keycloak.v2`

`infra/keycloak/themes/recallio/login/` : 72 Ko, des fichiers statiques.

| Fichier | Rôle |
|---|---|
| `theme.properties` | `parent=keycloak.v2`, les feuilles de style, `locales=fr`, l'icône, l'origine publique lue dans l'environnement |
| `resources/css/recallio.css` | l'allure : 14 Ko |
| `messages/messages_fr.properties` | les textes, au tutoiement : 144 messages |
| `footer.ftl` | **le seul gabarit** : les liens légaux |
| `resources/fonts/` | Nunito variable, sous-ensemble latin (39 Ko), et sa licence OFL |
| `resources/img/` | Rico, clair et sombre, et l'icône de l'onglet |

Il couvre les pages du type « login » : connexion, inscription, mot de passe oublié, adresse à vérifier, nouveau mot de
passe, mise à jour du profil, confirmation de suppression du compte, déconnexion, information, erreur, page expirée.
Les pages d'options que le royaume n'active pas (OTP, clés d'accès, fournisseurs d'identité) héritent de la même
feuille de style, sans avoir été regardées ; leurs textes restent ceux de Keycloak.

### 2. L'allure : les tokens de l'application, recopiés

`recallio.css` vient après les feuilles du parent et les surcharge. Elle recopie en variables CSS (`--rc-*`) les tokens
de `frontend/src/index.css`, clairs et sombres, et applique les règles de l'ADR 0058 : le fond de l'application, une
carte arrondie au bord épais en dessous, des champs au contour net de 2 px, le bouton principal violet en relief de
56 px qui s'enfonce au clic, des liens soulignés, un focus de 3 px, Nunito en 500 au moins. Rico et « Recallio » sont
au-dessus de la carte : le nom vient du royaume (`displayName`), Rico d'un pseudo-élément. Ni dégradé, ni émoji, ni
image de fond. Rien ne bouge pour qui a demandé moins d'animations.

**Les tokens sont recopiés à la main** : un token qui change dans `index.css` se reporte ici. Aucun test ne le
rappelle ; un commentaire en tête des deux blocs le dit.

### 3. Le thème sombre suit le système

`prefers-color-scheme`, et rien d'autre : la page de Keycloak est sur une autre origine et ne peut pas lire le choix
fait dans le profil de l'application. Quelqu'un qui a choisi le thème clair dans Recallio sur un système sombre verra
donc une page de connexion sombre.

### 4. Les textes : en français, au tutoiement

Le thème ne propose que le français (`locales=fr`), et le royaume local s'aligne sur celui de production
(`internationalizationEnabled`, `supportedLocales`, `defaultLocale`) : l'e2e voit ce que verra un utilisateur. Les
messages du thème parent qui vouvoient, ou qui parlent comme un serveur (« Soumettre », « Enregistrement »,
« courriel »), sont redonnés pour les pages couvertes. L'apostrophe y est typographique (’) : Keycloak passe ces textes
à `MessageFormat`, où l'apostrophe droite a un sens. « Se souvenir de moi » garde le texte de Keycloak : la politique
de confidentialité et la documentation le citent.

### 5. Les liens légaux : `footer.ftl`, et l'origine dans l'environnement

- Sur la **page d'inscription**, au pied de la carte : « Avant de créer ton compte, ce que Recallio garde sur toi, et
  qui l'édite : politique de confidentialité et mentions légales. », avec deux liens soulignés. C'est la phrase de
  l'essai sans compte (`LegalLinks`) : **elle informe, elle ne fait rien accepter**, comme l'ADR 0074 le décide. Pas de
  case à cocher.
- Sur **toutes les autres pages**, au même endroit : « Mentions légales » et « Confidentialité », discrets, comme au
  pied de l'application.
- L'origine vient de `PUBLIC_ORIGIN`, que le conteneur de Keycloak reçoit du compose (déjà en production, pour l'import
  du royaume ; ajoutée en local). `theme.properties` la lit (`${env.PUBLIC_ORIGIN}`). Si elle manque, `footer.ftl`
  n'écrit aucun lien plutôt qu'un lien cassé.

La phrase est sous le formulaire, pas au-dessus du bouton : c'est le prix de ne pas recopier `register.ftl`.

### 6. Le thème voyage par un volume

`infra/keycloak/themes/recallio` est monté en lecture seule dans `/opt/keycloak/themes/recallio` par le compose local
(donc l'e2e) et par celui de production (`../../keycloak/themes/recallio` : le serveur a tout le dépôt,
`docs/deploiement.md`). Les deux fichiers de royaume le nomment (`loginTheme`). Caddy n'a pas changé : les fichiers du
thème sont sous `/resources/`, qu'il laisse déjà passer.

### 7. Ce qui reste par défaut

- **Les e-mails de Keycloak** (vérification de l'adresse, mot de passe oublié) : l'allure et les textes de Keycloak, en
  français, qui vouvoient.
- **La console de compte** (`/realms/recallio/account/`) : celle de Keycloak. L'application n'y envoie personne.
- **Les Dev Services** (`./mvnw quarkus:dev`, les tests du backend) démarrent leur propre Keycloak avec le royaume
  local, sans le dossier du thème. Keycloak se replie alors sur son thème par défaut et écrit une erreur dans ses logs
  (« Failed to find LOGIN theme recallio, using built-in themes ») : la page de connexion y est celle de Keycloak, en
  français. Pour voir le thème : `docker compose -f infra/compose.yaml up`.

## Conséquences

- ➕ La page de connexion ressemble à l'application, dans les deux thèmes, et la tutoie.
- ➕ La page d'inscription mène aux deux pages légales : le manque laissé par l'ADR 0074 est comblé.
- ➕ Aucun gabarit de page recopié : une montée de version de Keycloak garde ses scripts et ses correctifs.
- ➕ Rien à construire ni à publier ; le thème se relit par pull request.
- ➖ **La feuille de style dépend des classes de PatternFly 5** que posent les gabarits du parent. Le jour où Keycloak
  change de version de PatternFly, ou de thème par défaut, la page perd son allure d'un coup, sans erreur. Le parcours
  e2e `keycloak-theme.spec.ts` (le titre, la police, les liens, axe-core dans les deux thèmes) en verra une partie ; le
  reste se regarde.
- ➖ **Les tokens sont en double** (`index.css` et `recallio.css`).
- ➖ **Un thème introuvable ne fait pas échouer Keycloak** : il sert le thème par défaut. La CI vérifie donc que le
  thème nommé par le royaume existe et que le compose le monte (job `production`).
- ➖ **Un royaume déjà importé ne relit pas son fichier**, et **Keycloak garde les thèmes en cache** en production : le
  thème se choisit alors dans la console, et une mise à jour du thème demande de redémarrer Keycloak
  (`docs/deploiement.md`, section 7).
- ➖ Le thème sombre ne suit pas le choix fait dans le profil.
- ➖ La page d'information a parfois un titre vide (le gabarit du parent, quand Keycloak ne lui en donne pas).
- 🔁 **À chaque montée de version mineure de Keycloak** (la pull request de Dependabot) : la CI joue
  `keycloak-theme.spec.ts` et `account.spec.ts` sur la nouvelle image. S'ils échouent, vérifier dans le jar des thèmes
  de l'image que `template.ftl` importe toujours `footer.ftl` et appelle sa macro `content`, que `pageId` vaut toujours
  `register` sur la page d'inscription, et que le thème parent s'appelle toujours `keycloak.v2`. Puis regarder la page
  de connexion et celle d'inscription, en clair et en sombre.
- 🔁 Les e-mails au ton de l'application (un thème `email`), si les testeurs de la bêta les trouvent froids.
