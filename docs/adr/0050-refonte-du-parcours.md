# 0050. La refonte du parcours : navigation, accueil, essai sans compte, rétention

- Statut : accepté
- Date : 2026-11-05
- Complète : ADR 0049 (le design system « Coach »), ADR 0048 (la session de révision), ADR 0037 (le Tour)

## Contexte

L'ADR 0049 a donné à l'application son identité visuelle, mais la structure des écrans n'avait pas changé. Le propriétaire
demandait une interface qui suive les principes d'IHM et les pratiques qui retiennent les utilisateurs, et que lui soient
signalés les ajouts utiles. Voici ce que montrait l'application avant cette refonte :

- **La navigation** : de petits liens en haut de l'écran, hors de portée du pouce sur un téléphone, sans le Tour, et
  « Se déconnecter » au même rang que « Réviser ».
- **L'accueil** : un tableau de bord, sans action principale. Les chiffres venaient avant le bouton qui lance la révision.
- **Avant le compte** : on ne voyait rien de ce que vaut l'application. Le premier geste demandé était de créer un
  compte, ce qui fait partir la plupart des visiteurs.
- **Les rappels et l'installation** : rangés dans le profil, que personne ne visite.
- **Le Tour** : le cours, puis le code, l'un sous l'autre. Sur un ordinateur, on remontait sans cesse pour relire la
  consigne.
- **La rétention** : rien ne la mesurait, alors que c'est le critère de la phase 1 (« combien reviennent après 7 jours »).
- **Le catalogue** : 19 sujets sortis du document Java (ADR 0025) y étaient publiés. Ce ne sont que des chapitres d'entretien,
  pas encore des dossiers de notions : un visiteur les prenait pour des cours complets.
- **Le ton** : le vouvoiement, à côté des « À toi » du Tour.

Les principes appliqués :

- **Loi de Fitts** : les cibles fréquentes sont grandes et près du pouce.
- **Loi de Hick** : une seule action principale par écran.
- **Gradient d'objectif** : plus on est près du but, plus on a envie de le finir.
- **Règle du pic et de la fin** : on se souvient d'une expérience par son meilleur moment et par sa fin.
- **Demander une permission au bon moment** : web.dev recommande de poser d'abord sa propre question, puis d'ouvrir la
  fenêtre du navigateur seulement si la réponse est oui. Un refus du navigateur est définitif ; le nôtre ne l'est pas.
- **Montrer la valeur avant de demander un compte**.

## Options envisagées

**La navigation** :

- **Une barre en haut** (l'existant) : rien à changer, mais loin du pouce et chargée.
- **Un menu « hamburger »** : tout tient, mais ce qu'on ne voit pas, on ne l'utilise pas.
- **Une barre d'onglets en bas sur mobile, un menu sur ordinateur** (retenu) : cinq destinations, la convention des
  applications mobiles.

**L'essai sans compte** :

- **Des comptes invités** : la progression de l'invité passe dans son compte à l'inscription. C'est le plus fluide, mais il
  faut des identités anonymes, la fusion de deux progressions, et des garde-fous contre les comptes jetables.
- **Un essai sans état** (retenu) : cinq questions corrigées par le serveur, rien n'est enregistré, puis l'invitation à
  créer son compte.
- **Rien** : le compte reste le premier geste.

**La mesure de la rétention** :

- **Un outil d'analyse** (Plausible, PostHog...) : riche, mais il faut un bandeau de consentement, un sous-traitant de
  plus au registre RGPD, et il compte des visites, pas des révisions.
- **Un calcul sur nos données** (retenu) : `daily_activity` dit déjà qui a révisé quel jour.

**L'objectif choisi à l'inscription** :

- **Un objectif de cartes par utilisateur** : l'objectif fixe le streak et la régularité des classements de groupe (ADR
  0018). Le rendre personnel demande une migration, et un classement entre joueurs aux objectifs différents.
- **Un temps par jour** (retenu) : 5, 10, 15 ou 20 minutes. Il devient la durée des sessions (ADR 0048) ; l'objectif de
  cartes reste commun.

**La performance** :

- **Découper l'application route par route** : le gain le plus fort, mais toutes les pages se chargeraient en deux temps,
  et une bonne part des tests devraient attendre.
- **Ne garder que les grammaires de highlight.js du catalogue** : essayé. Sans effet, car `rehype-highlight` importe
  toujours l'ensemble « courant » de lowlight.
- **Compresser les fichiers servis et fixer un budget** (retenu) : nginx ne compressait rien. Le JavaScript passait sur le
  réseau à 1 Mo au lieu de 285 Ko.

## Décision

1. **La navigation**.
   - Cinq destinations : Accueil, Réviser, Apprendre (le catalogue et les Tours), Classé, Groupes.
   - Une barre fixe en bas sur mobile (cibles de 56 px), un menu dans l'en-tête sur ordinateur.
   - Le profil est sous le nom de l'utilisateur, dans l'en-tête ; « Se déconnecter » est dans le profil.
   - Pendant une session, la barre du bas disparaît (mode concentration).
2. **L'accueil à une action**.
   - Une seule action en tête : « Continuer », sur le dernier sujet révisé, avec la durée et les questions de la dernière
     session (`/review?subject=java&minutes=10&mode=all`, qui lance la session sans écran de réglage).
   - Un anneau montre l'objectif du jour, et une phrase dit ce qui manque pour tenir sa série.
   - Viennent ensuite le Tour des sujets suivis, les quêtes, puis la progression.
3. **Une fin de session qui marque**.
   - Une célébration quand l'objectif du jour est atteint.
   - Le bilan : l'XP gagnée, les cartes réussies, l'objectif.
   - « Encore 5 minutes », pour prolonger d'un geste.
4. **La révision au geste et au clavier**.
   - Les gestes : sur un écran tactile, on glisse la carte retournée à droite (« Je savais ») ou à gauche (« Je ne savais
     pas »), au-delà de 96 px.
   - Le clavier : Espace ou Entrée retourne la carte, 1 et 2 répondent, annoncés par `aria-keyshortcuts`. Les raccourcis
     ne prennent ni la frappe dans un champ, ni Espace et Entrée sur un bouton qui a le focus.
   - Le verdict d'une réponse corrigée s'anime, sauf pour qui demande moins de mouvement (`prefers-reduced-motion`).
   - Un son et une vibration, coupés par défaut, s'allument dans le profil : beaucoup révisent au bureau ou dans les
     transports.
5. **Le Tour en deux colonnes** sur un écran large.
   - Le cours est à gauche, avec « Teste-toi » sur la dernière page : deux questions de la notion, à retourner.
   - Le code et l'« À toi » sont à droite, et restent visibles quand on fait défiler le cours.
   - Sur un téléphone, les deux s'empilent.
6. **L'essai sans compte**.
   - La page `/try/:sujet`, proposée sur l'accueil d'un visiteur.
   - `GET /api/v1/subjects/{sujet}/trial` : cinq questions, une par notion essentielle (priorité 1), du débutant vers
     l'expert, seulement dans des formats corrigés par le serveur (vrai ou faux, QCM, code à compléter).
   - `POST /api/v1/subjects/{sujet}/trial/{notion}/checks` corrige une réponse comme l'entraînement (ADR 0038), sans rien
     enregistrer.
   - Les deux routes sont publiques, comme le catalogue. Un sujet sans notion n'a pas d'essai (404).
   - Le catalogue dit quels sujets ont un Tour (`hasTour`), pour proposer leur essai et leur Tour.
7. **L'inscription en deux questions** : les sujets, puis le temps par jour. La première session part aussitôt, sur le
   premier sujet choisi : on révise dès la première minute.
8. **Les demandes au bon moment**.
   - **Le rappel** : demandé à la fin d'une session où l'on a révisé, jamais à l'arrivée. D'abord notre question (« Je te
     rappelle demain à 19 h ? »), puis la fenêtre du navigateur si la réponse est oui. Après « Plus tard », la question ne
     revient pas avant 14 jours.
   - **L'installation** : proposée à la fin d'une session, à partir de la deuxième visite. Chrome et Edge annoncent
     qu'elle est possible (`beforeinstallprompt`) : on garde l'annonce pour ce moment. Sur iPhone, Safari n'annonce
     rien, donc on explique le menu Partager.
9. **Le tutoiement partout**, dans l'interface comme dans les messages d'erreur du backend. Plus de petits libellés en
   majuscules au-dessus des titres : une casse de phrase.
10. **Les sujets sortis du document Java passent en brouillon**. Ils restent visibles des admins, et seront publiés une
    fois réécrits en dossiers de notions (ADR 0026). Java et Go restent publiés.
11. **L'accessibilité et la performance contrôlées en CI**.
    - `e2e/tests/accessibility.spec.ts` passe axe-core (`@axe-core/playwright`) sur les pages d'un visiteur et d'un
      utilisateur connecté, carte de révision comprise. Il échoue sur une violation grave ou critique des critères WCAG
      2.2 A et AA.
    - nginx compresse ce qu'il sert (gzip, niveau 6).
    - `npm run size` échoue, dans le job `frontend`, si le JavaScript chargé à l'ouverture dépasse 300 Ko compressés
      (284 Ko aujourd'hui).
12. **La rétention mesurée**.
    - `GET /api/v1/admin/retention`, réservé aux admins, et la page `/admin/retention`, liée depuis le profil.
    - La mesure : la part des joueurs revenus réviser 1, 7 et 30 jours après leur premier jour actif, jour pour jour.
    - Un joueur compte quand son premier jour est assez ancien pour qu'on puisse le mesurer.

## Conséquences

- ➕ Un visiteur répond à cinq questions avant de donner son adresse, et un nouvel inscrit révise dans la minute.
- ➕ Une action principale par écran, à portée du pouce. Les demandes de permission arrivent quand leur intérêt est
  évident, et un refus de notre part n'est pas définitif.
- ➕ La rétention se lit dans l'application, sans outil tiers ni bandeau de consentement.
- ➕ Le JavaScript passe à 285 Ko sur le réseau au lieu de 1 Mo, et un ajout trop lourd se voit dans la pull request.
- ➖ Deux routes publiques de plus. Elles ne lisent que du contenu déjà public et n'écrivent rien, mais une limite de
  débit reste à poser au déploiement, comme pour le catalogue.
- ➖ La durée des sessions, les sons et le rappel reporté sont retenus sur l'appareil, pas sur le compte : ils ne suivent
  pas l'utilisateur sur un autre appareil.
- ➖ La rétention compte des joueurs (un premier jour actif), pas des inscriptions : un inscrit qui n'a jamais révisé n'y
  apparaît pas.
- ➖ Les 19 sujets en brouillon disparaissent du catalogue public jusqu'à leur réécriture.
- 🔁 Le JavaScript chargé à l'ouverture approche de son budget. Si le budget est atteint, la prochaine étape est de
  charger à la demande le rendu Markdown et highlight.js (environ 100 Ko compressés), dont ni l'accueil ni le tableau de
  bord n'ont besoin.
