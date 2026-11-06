# 0052. Les pages Cours, Tour et Révisions, et un premier login guidé par le but

- Statut : accepté
- Date : 2026-11-06
- Remplace en partie : ADR 0050 (la navigation, point 1 ; l'inscription en deux questions, point 7)
- Complète : ADR 0013 (les catégories), ADR 0032 (les parcours), ADR 0037 (le Tour)

## Contexte

En testant l'application en local, le propriétaire a demandé trois pages au lieu de l'écran « Quels sujets veux-tu
réviser ? » : Cours, Tour et Révisions. Chacune s'ouvre sur les catégories, avec le logo de chaque sujet. Java et Go
s'ouvrent ; les autres sujets affichent « work in progress ».

Quatre constats allaient dans le même sens :

- **Personne n'orientait un débutant.** Le premier login demandait des sujets et un temps par jour, jamais un but. Le
  parcours par défaut, « préparer un entretien », range les notions par priorité d'entretien, pas par ordre
  d'apprentissage.
- **La navigation était pleine.** L'ADR 0050 lui donnait cinq destinations : Accueil, Réviser, Apprendre, Classé et
  Groupes. Trois pages de plus en feraient sept, bien trop pour une barre en bas d'un téléphone.
- **Le catalogue taisait ce qui se prépare.** Les brouillons y étaient cachés, et les langages prévus (Python, C...)
  n'existaient nulle part.
- **La sécurité n'avait pas de catégorie** : elle était rangée dans les Fondamentaux.

## Options envisagées

**La navigation** :

- **Six ou sept entrées** : trop pour un téléphone.
- **Un menu « Plus »** : il range ce qu'on ne voit plus.
- **Réunir le classé et les groupes dans « Jouer »** (retenu) : ce qui se joue contre les autres.

**Les sujets annoncés** :

- **Une liste dans le front** : annoncer un sujet demanderait du code.
- **Un fichier de feuille de route** : un format de plus à lire et à importer.
- **Un dossier de sujet qui n'a que son `subject.yaml`, en brouillon** (retenu) : rien à coder, et le même dossier se
  remplit quand on rédige le sujet.

**La page Cours** :

- **Le catalogue actuel** : des thèmes et des cartes, pas un cours.
- **Les leçons, notion par notion, avec sa maîtrise** (retenu) : la carte des compétences existe déjà ; ses notions
  deviennent des liens vers leur leçon.

**Les logos** :

- **Des icônes génériques seulement** : rien ne distingue Java de Go.
- **Les logos des langages et des outils** (retenu), pris chez devicon (licence MIT). Un sujet sans logo garde une
  icône du design system (ADR 0049).

## Décision

1. **La navigation** : Accueil, Cours, Tour, Révisions et Jouer.
   - Chaque entrée reste allumée sur les pages qu'elle ouvre : le cours d'un sujet sous Cours, un duel sous Jouer.
   - Jouer mène au classé et aux groupes, avec leurs défis et leurs parties live.
2. **Les trois pages s'ouvrent sur les sujets**, rangés par catégorie, chacun avec son logo (`SubjectGallery`).
   - Un sujet s'ouvre s'il est publié, ou pour un admin qui relit un brouillon, et s'il a ce que la page propose : des
     cartes pour Cours et Révisions, un Tour pour le Tour.
   - Les autres sont grisés, marqués « En préparation » ; les toucher affiche « Work in progress ».
3. **Cours** : le cours d'un sujet (`/subjects/:slug/course`), qui est l'ancienne carte des compétences.
   - Le titre de chaque notion ouvre sa leçon (`/subjects/:slug/course/:notion`).
   - La leçon vient de `GET /api/v1/subjects/{sujet}/notions/{notion}`, publique comme le catalogue : son texte, s'il
     existe, et les notions d'avant et d'après dans le référentiel.
   - L'ancienne page « Apprendre » devient « Toutes les cartes », ouverte depuis Cours.
4. **Révisions** : le sujet, puis la durée et les questions. Réviser un sujet le fait suivre : il entre au tableau de
   bord.
5. **Les sujets annoncés** :
   - `GET /api/v1/subjects?upcoming=true` liste aussi les brouillons, pour tous. Leurs thèmes, leurs cartes et leur Tour
     restent réservés aux admins.
   - Un sujet annoncé est un dossier qui n'a que son `subject.yaml`, en brouillon : Python, TypeScript, Kotlin, C#, C,
     C++ et Rust pour commencer.
   - Les contrôles du contenu l'acceptent ; un sujet publié a toujours un référentiel et des cartes.
6. **La catégorie Cybersécurité** (`security`) s'ajoute à la liste fermée (CLAUDE.md, section 2.5). Le sujet Sécurité y
   passe, depuis les Fondamentaux.
7. **Les logos** viennent de devicon (version 2.16.0, licence MIT), dans `frontend/public/logos/` : des fichiers
   servis tels quels, hors du JavaScript de l'ouverture.
   - Ils sont posés sur un fond clair, dans les deux thèmes, pour qu'un logo sombre (Rust) reste lisible.
   - Ils ne servent qu'à désigner le sujet qui porte leur nom.
   - Un sujet sans logo a une icône Phosphor : la sienne, ou celle de sa catégorie.
8. **Le premier login demande le but** : « Qu'est-ce qui t'amène ? »
   - « Je n'ai jamais programmé » : il apparaît une fois « Programmer de zéro » publié, et ouvre son Tour.
   - « Je connais déjà un autre langage » : on choisit le langage, et son Tour s'ouvre.
   - « Je prépare un entretien » : on choisit le sujet, puis la durée de sa première session.
   - Les deux premiers buts mettent le sujet en parcours « apprendre » (ADR 0032).
   - Le temps par jour choisi à l'inscription (ADR 0050, point 7) disparaît : la durée se choisit avant chaque session.
9. **L'accueil** : qui apprend un sujet qui a un Tour le reprend d'abord, avant sa révision.

## Conséquences

- ➕ Un visiteur voit ce qui s'ouvre déjà et ce que Recallio prépare.
- ➕ Un débutant est conduit au Tour, dans le parcours fait pour apprendre.
- ➕ Annoncer un sujet ne demande qu'un `subject.yaml`, relu par pull request comme le reste du contenu.
- ➖ Les logos sont les marques de leurs propriétaires. Avant d'ouvrir l'application à tous, il faut relire les règles
  de marque des plus exigeants (Oracle pour Java, la Python Software Foundation pour Python).
- ➖ Le cours d'un sujet demande un compte, puisqu'il montre la maîtrise ; la leçon d'une notion se lit sans compte.
- 🔁 Le Tour et la révision restent à relier : la prochaine étape fait entrer une notion en révision une fois sa page
  du Tour lue.
