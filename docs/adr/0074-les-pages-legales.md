# 0074. Les pages légales : deux pages du front, exactes sur les faits, et un éditeur dont les coordonnées restent hors du dépôt

- Statut : accepté
- Date : 2026-11-24
- Prolonge : ADR 0069 (une seule image du front, configurée à son démarrage), ADR 0056 (télécharger ses données,
  supprimer son compte), ADR 0064 (le parcours mesuré, sans identifiant), ADR 0065 (le plan du site), ADR 0070 (des logs
  sans donnée personnelle, Sentry), ADR 0071 (Keycloak, Brevo), ADR 0073 (les sauvegardes), ADR 0054 (la
  Content-Security-Policy), ADR 0058 (le design system)
- Origine : le déploiement (phase 1, étape 7), son étape 6, « les pages légales »

## Contexte

Avant d'ouvrir la bêta, le site doit dire qui l'édite et ce qu'il fait des données de ses utilisateurs : des mentions
légales (loi n° 2004-575 du 21 juin 2004 pour la confiance dans l'économie numérique, article 1-1) et une information
sur les données personnelles (RGPD, articles 13 et 14). Les ADR 0056, 0071 et 0073 y renvoyaient déjà : la durée de
conservation des sauvegardes, l'adresse IP que garde Keycloak.

Les contraintes :

- **le dépôt est public, et l'éditeur est un particulier** : ni son nom ni son adresse ne doivent y être écrits ;
- **une seule image du front pour tous les environnements** (ADR 0069) : rien de propre à la production n'entre au
  build ;
- **ni le propriétaire ni Claude ne sont juristes** : les textes ne peuvent promettre que ce que le code fait, et
  doivent être relus ;
- **l'inscription se fait sur la page de Keycloak**, dont le thème est une étape ultérieure : l'application ne peut
  informer qu'avant d'y envoyer, et au retour.

## Options envisagées

**Où vit le texte** :

- **Des fichiers Markdown rendus par `react-markdown`** : faciles à relire, mais la page ne peut alors pas dépendre de
  la configuration (un éditeur nommé ou non), et ses tableaux sortent du design system.
- **Des pages statiques servies par nginx, hors de la PWA** : lisibles sans JavaScript, mais sans le layout, les thèmes
  ni le pied de page, et un second jeu de styles à tenir.
- **Un générateur de politique en ligne** : un texte générique, qui affirmerait des choses que le code ne fait pas.
- **Des composants React** (retenu) : le texte est du code, relu par pull request, daté dans la page, et testé.

**Les coordonnées de l'éditeur** :

- **En dur dans le code** : le nom d'un particulier dans un dépôt public, pour toujours dans son historique.
- **Au build (`VITE_…`)** : une image par environnement, ce que l'ADR 0069 a écarté.
- **Une route du backend** : un appel de plus, et une configuration de plus côté backend, pour deux chaînes.
- **Les balises d'`index.html`, remplies par nginx au démarrage** (retenu) : le mécanisme de l'adresse OIDC et de
  Sentry (`servedConfig.ts`).

**Publier ou non le nom de l'éditeur** :

- **Toujours** : la loi ne l'impose pas à un particulier qui édite à titre non professionnel.
- **Jamais** : l'éditeur peut vouloir signer son projet de portfolio.
- **Au choix, par une variable** (retenu) : `LEGAL_PUBLISHER`, facultative.

**Les prestataires facultatifs** (Sentry) :

- **Piloter la ligne par la configuration servie** (`SENTRY_DSN` du front) : le backend et le runner ont chacun leur
  DSN ; la ligne pourrait disparaître alors que le backend envoie encore ses erreurs.
- **Une formulation vraie dans les deux cas** (retenu) : « quand le suivi des erreurs est activé ».

**Un bandeau de consentement** :

- **Un bandeau** : il n'aurait rien à faire accepter.
- **Pas de bandeau** (retenu) : voir « Cookies et traceurs », ci-dessous.

**Une case à cocher à l'inscription** : la politique informe, elle ne demande pas un consentement (les traitements
reposent sur le service demandé et l'intérêt légitime ; seuls les rappels reposent sur le consentement, demandé par le
navigateur). Une case « j'accepte » ferait croire le contraire. Écartée.

## Décision

### 1. Deux pages publiques, en composants React

- **`/mentions-legales`** (`src/legal/LegalNoticePage.tsx`) et **`/confidentialite`** (`PrivacyPolicyPage.tsx`),
  chargées à la demande comme les autres pages. Leurs adresses sont en français, comme leur texte : ce sont celles
  qu'un lecteur français cherche, et les seules adresses de la PWA à être tapées ou citées telles quelles (les autres,
  `learn`, `tours`, `play`, restent en anglais).
- Lisibles sans compte, laissées aux moteurs par `robots.txt` (`seo.test.ts` le vérifie), et **dans le plan du site** :
  `SitemapService.FIXED_PATHS` (ADR 0065). Leur titre d'onglet vient de leur `h1` (`usePageTitle`) ; leurs balises de
  partage sont celles de toutes les pages.
- **La date de la dernière mise à jour** est une constante de chaque page (`UPDATED_ON`), affichée sous le titre : qui
  modifie le texte la change.
- Le style : des composants de texte propres à ces pages (`LegalText.tsx`), avec les tokens du design system. Une ligne
  fait au plus 65 caractères. **Un tableau** vit dans une zone qui défile de côté s'il ne tient pas, que le clavier
  atteint (`role="region"`, `tabIndex`, nommée par la légende du tableau) : la page, elle, ne défile jamais de côté.
  Un lien dans une phrase est souligné.

### 2. Les coordonnées de l'éditeur, données au conteneur du front

| Variable | Obligatoire | Rôle |
|---|---|---|
| `LEGAL_CONTACT_EMAIL` | en production (`:?` dans le compose) | L'adresse de contact : droits sur les données, signalement d'un contenu |
| `LEGAL_PUBLISHER` | non | Le nom de l'éditeur. Vide : il n'est pas publié |

- nginx les met dans deux balises d'`index.html` (`recallio-legal-publisher`, `recallio-legal-contact-email`), par
  `sub_filter`, dans la seule `location` qui sert les fichiers ; `src/legal/legalConfig.ts` les lit par
  `servedConfig.ts`.
- **Sans nom**, les mentions légales disent que le site est édité par un particulier, à titre non professionnel, qui a
  communiqué son identité à l'hébergeur : c'est ce que permet l'**article 1-1, II** de la loi n° 2004-575, dans sa
  version issue de la loi n° 2024-449 du 21 mai 2024 (avant elle, la règle était à l'article 6, III, 2). L'hébergeur,
  lui, est nommé, avec son adresse.
- **L'hébergeur est écrit en dur**, c'est un fait public : OVH SAS, 2 rue Kellermann, 59100 Roubaix, France (RCS Lille
  Métropole 424 761 419, d'après les mentions légales de ses sites). Aucun numéro de téléphone : celui que publie
  OVHcloud n'a pas pu être vérifié, et l'article 1-1, II ne demande que le nom et l'adresse.
- **Sans adresse de contact** (`npm run dev`, sans nginx), la page le dit (« non configurée sur cette installation »)
  plutôt que d'en inventer une.
- **L'échappement.** La valeur traverse deux lectures : celle de nginx (une chaîne entre apostrophes de `sub_filter`,
  où `$` annonce une variable et `\` échappe) puis celle du navigateur (un attribut HTML entre guillemets). Un script
  de l'image, `frontend/nginx/18-escape-legal-values.envsh`, que l'image de nginx source avant de remplir son gabarit,
  exporte `LEGAL_PUBLISHER_HTML` et `LEGAL_CONTACT_EMAIL_HTML` : `&`, `<`, `>`, `"`, `'`, `\` et `$` y sont des
  références de caractère HTML, et les retours à la ligne en sont retirés. nginx n'y voit que du texte, le navigateur
  rend le caractère, et React l'affiche comme du texte. Un nom avec une apostrophe, un guillemet ou une balise ne casse
  ni la configuration ni la page.
- **La Content-Security-Policy** ne change pas : aucune origine de plus, aucun script en ligne. Les deux liens externes
  (ovhcloud.com, cnil.fr) sont des navigations, qu'elle ne régit pas.
- **Les valeurs** : `infra/production/app/.env.example` (vides, à remplir) ; `infra/compose.yaml`, que le compose de
  l'e2e surcharge sans les changer (`contact@recallio.example`, et un nom avec une apostrophe, que le parcours
  `legal.spec.ts` relit).

### 3. Les liens

- **Un pied de page**, dans le layout, sur toutes les pages, visiteur ou connecté : « Mentions légales » et
  « Confidentialité ». Il n'existait aucun pied de page. Sur un téléphone, sa marge du bas le tient au-dessus de la
  barre de navigation ; **en mode concentration** (une session, un duel, un examen), il disparaît avec elle.
- **À l'entrée**, une phrase et les deux liens, sans case à cocher : sous « Me connecter ou créer un compte » de la page
  publique, sous « Créer mon compte » du bilan de l'essai, et à l'écran du premier login.
- **Dans le profil**, section « Mes données » : un lien vers la politique.
- La page d'inscription de Keycloak ne les porte pas encore : c'est l'étape du thème.

### 4. Ce que dit la politique, et ce qui le prouve

La politique dit ce que le code fait. Chaque affirmation a sa preuve ; une donnée, un stockage ou un prestataire de plus
se reporte dans la page, avec la date.

| Affirmation | Preuve |
|---|---|
| La visite : une ligne sans identifiant, sans adresse IP ni cookie | `V30__funnel_event.sql` (trois colonnes), ADR 0064 |
| L'essai : réponses non enregistrées, une ligne « essai commencé » | ADR 0050, 0064 ; `TrialService` n'a pas de dépôt |
| Le journal du serveur web : ni adresse IP, ni navigateur, ni page d'origine, ni paramètres ; jeton et adresse de profil masqués | `log_format recallio_json` et les `map` de `default.conf.template` ; ADR 0070 ; Caddy sans journal d'accès (`Caddyfile`, ADR 0071) |
| Les journaux tournent à 50 Mo par service | `x-logging` du compose de production (pilote `local`, 5 fichiers de 10 Mo) |
| L'adresse IP : en mémoire pour la limite de débit ; écrite dans le journal d'erreurs de nginx lors d'une erreur rare | `limit_req_zone` (ADR 0054) ; ADR 0070, conséquences |
| L'identité chez Keycloak : nom d'utilisateur, e-mail, prénom, nom, mot de passe haché | Le royaume de production (`registrationEmailAsUsername: false`, profil par défaut de Keycloak) ; `V3__app_user.sql` (« ni mot de passe ni e-mail ») |
| Les sessions : l'adresse IP de la connexion, 30 minutes et 10 heures, ou 30 et 90 jours | Le code de Keycloak (`PersistentUserSessionAdapter`, qui garde `ipAddress`) ; le royaume (`ssoSession…RememberMe`), ADR 0071 |
| L'adresse IP du dernier échec de connexion | Le code de Keycloak (`LoginFailureEntity.lastIPFailure`), ADR 0071 |
| Le profil : nom affiché, fuseau, sujets suivis, parcours | `V3__app_user.sql`, `V7`, `V18` ; `TokenIdentity` ; `UserPersonalData` |
| La progression | `ReviewPersonalData` : `review_state`, `review_log`, `mock_exam`, `tour_progress`, `tour_last_page`, `known_notion`, `interview_plan`, `daily_challenge_answer`, `drill_activity` (le compteur du jour de la révision intensive, ADR 0078) |
| L'XP, l'activité, les quêtes, les succès | `GamificationPersonalData` |
| Les groupes, le classé | `GroupPersonalData`, `LivePersonalData`, `RankedPersonalData` |
| Le profil public, et ce qu'il ne montre jamais | `V25__public_profile.sql`, ADR 0043 |
| Les rappels : adresse, deux clés, heure, dernier rappel ; oubliés quand l'appareil est désabonné | `V28__push_subscription.sql`, ADR 0047 |
| Les signalements, gardés après la suppression, auteur jamais montré | `FeedbackPersonalData`, ADR 0042 |
| Le code : envoyé sans identité, non gardé | `RunnerClient.RunRequest`, `ProgramsClient.ProgramRequest` (un langage et des fichiers) ; ADR 0030, 0072 |
| Le stockage local, le stockage de session | Les sept appels à `localStorage` et `sessionStorage` du front (`theme.ts`, `installPrompt.ts`, `sessionSettings.ts`, `feedback.ts`, `ReminderPrompt.tsx`, `funnel.ts`, `routes.tsx`), `oidc.ts` (jetons en `sessionStorage`), `oidc-client-ts` (état d'une connexion en `localStorage`) |
| Le cache : les fichiers de l'application, aucune réponse de l'API | `vite.config.ts` (`globPatterns`, aucun `runtimeCaching`) |
| L'application ne pose aucun cookie | Aucun `document.cookie` dans le front, aucun `Set-Cookie` dans le backend |
| Aucun traceur, aucune publicité, aucun outil de mesure tiers | `package.json` ; la Content-Security-Policy n'admet que l'application, le fournisseur OIDC et Sentry (ADR 0054, 0070) ; polices servies par l'application |
| Aucune suppression pour inactivité | Rien dans le code ne le fait |
| OVHcloud, Backblaze, Brevo, Sentry, les services de notification | ADR 0069, 0073, 0071, 0070 ; `PushSubscription` (la liste fermée des services) |
| Sentry : ni identité, ni cookies, ni paramètres ; adresse IP non gardée | ADR 0070 (`dataCollection`, `scrub.ts`, `SentryEventScrubber`, « Prevent Storing of IP Addresses ») |
| Les sauvegardes : chaque nuit, chiffrées, 30 jours, 32 au plus ; suppressions rejouées | ADR 0073 |
| Télécharger, supprimer ; ce qui reste sous « Compte supprimé » ; l'identité confirmée chez Keycloak | ADR 0056, `AccountDataSection.tsx`, `PersonalData.DELETED_ACCOUNT_NAME` |

Trois choses que les ADR ne disaient pas, trouvées dans le code et écrites dans la page :

- **Keycloak garde l'adresse IP de chaque session de connexion**, le temps de la session (jusqu'à 90 jours avec « Se
  souvenir de moi »), en plus de celle du dernier échec ;
- **`oidc-client-ts` écrit l'état d'une connexion en cours dans `localStorage`**, le temps de l'aller-retour ;
- **l'inscription demande un nom d'utilisateur, un prénom et un nom**, en plus de l'adresse e-mail.

### 5. Cookies et traceurs : pas de bandeau

L'application ne pose aucun cookie. Ce qu'elle écrit sur l'appareil ne sert qu'à elle : les jetons de la session
(`sessionStorage`), les réglages de l'interface (thème, sons, dernière session), le cache hors ligne, et deux repères
locaux (le nombre d'ouvertures, pour ne proposer l'installation qu'à la deuxième ; la date d'un « Plus tard » aux
rappels). Les cookies de Keycloak sont ceux de la session de connexion. Ce sont les traceurs que les lignes directrices
de la CNIL sur les cookies et autres traceurs (délibération n° 2020-091 du 17 septembre 2020) exemptent du
consentement : l'authentification auprès d'un service, et la personnalisation de l'interface à la demande de
l'utilisateur. D'où l'absence de bandeau, que la page explique.

Un point reste à faire confirmer par un professionnel : **le drapeau « visite comptée »** (`sessionStorage`,
ADR 0064) sert une mesure, pas le service. Il ne quitte pas le navigateur, ne dure que la session de l'onglet, et la
mesure n'a aucun identifiant ; la page le déclare (« visite déjà comptée »). S'il fallait l'écarter, la visite se
compterait sans lui, au prix d'un compte moins juste (ADR 0064 le prévoit déjà quand le stockage est refusé).

### 6. Des textes à relire

Les deux pages sont exactes sur les faits, pas validées en droit. **Le propriétaire les relit avant la mise en
service**, avec la liste de `docs/deploiement.md` (section 13), et **les fait relire par un professionnel avant
l'ouverture à tous**. En particulier :

- la qualité d'éditeur **non professionnel**, qui fonde l'anonymat : elle ne tiendra plus avec l'offre entreprise
  (phase 4) ou toute activité payante ;
- les **bases légales** retenues, et l'**âge** (15 ans, sans vérification) ;
- les **prestataires américains** (Backblaze, Sentry, les services de notification) et ce qu'il faut en dire ;
- l'absence de **licence** dans le dépôt, que les mentions légales constatent.

## Conséquences

- ➕ Le site dit qui l'édite, qui l'héberge, ce qu'il garde, pourquoi et combien de temps, sans compte, depuis toutes
  les pages ; et le dépôt public ne porte aucune coordonnée du propriétaire.
- ➕ Une seule image du front, toujours : deux variables de plus au démarrage du conteneur.
- ➕ Le texte se relit et se date par pull request ; son historique est public, ce que la page dit.
- ➖ **La politique recopie le code** : une nouvelle table avec un `user_id`, un nouveau stockage du navigateur ou un
  nouveau prestataire se reporte à la main dans la page et dans le tableau des preuves. `AccountResourceTest`
  (ADR 0056) rappelle déjà la table ; rien ne rappelle la page.
- ➖ **Le service worker garde `index.html`** : après un changement de `LEGAL_PUBLISHER` ou de `LEGAL_CONTACT_EMAIL`
  sans nouvelle image, un visiteur déjà venu voit l'ancienne valeur jusqu'à la version suivante. C'est déjà vrai de
  l'adresse OIDC et du DSN de Sentry (ADR 0069, 0070). Un changement de contact se fait donc avec une mise à jour.
- ➖ L'adresse de contact est lisible dans le code source de chaque page : il faut une adresse dédiée, pas l'adresse
  personnelle du propriétaire.
- ➖ La page d'inscription de Keycloak ne renvoie pas encore aux deux pages : quelqu'un qui s'y rend par un lien direct
  ne les voit qu'au retour, à l'écran du premier login.
- ➖ Les pages sont en français seulement, comme l'application.
- 🔁 Les liens sur la page d'inscription, avec le thème de Keycloak ; la relecture par un professionnel, des conditions
  d'utilisation et le choix d'une licence, avant l'ouverture à tous ; un éditeur professionnel (nom, adresse, téléphone,
  immatriculation publiés) avec l'offre entreprise.
