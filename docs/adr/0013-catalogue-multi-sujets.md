# 0013. Catalogue multi-sujets : le sujet dans l'identité des cartes, un en-tête par document

- Statut : accepté
- Date : 2026-10-30

## Contexte

Recallio devient un catalogue de sujets (CLAUDE.md, section 2.5) : Java, Go, puis Spring Boot, Kubernetes,
Python... Chaque sujet a son document de référence, au même format. L'import d'origine (ADR 0009) supposait un seul
document, ce qui pose trois problèmes dès le deuxième :

1. **Collision d'identifiants** : la clé d'un thème est son numéro de chapitre (`01`). Le chapitre 1 de Go aurait
   écrasé celui de Java.
2. **Retraits en cascade** : l'import retire toute carte absente du document. Importer Go aurait retiré les 1 311 cartes
   Java.
3. **Slugs en double** : les deux glossaires (et les deux chapitres de questions pièges) auraient le même slug.

Il faut aussi décider où décrire un sujet (titre, catégorie, ordre, brouillon ou publié).

## Options envisagées

Pour la description des sujets :

1. **Manifeste `content/subjects.yaml`** listant les sujets et leurs fichiers
   - ✅ Vue d'ensemble du catalogue en un fichier
   - ❌ Une dépendance de parsing YAML, et un second fichier à garder synchronisé avec les documents
   - ❌ L'import par l'admin (un document envoyé à l'API) ne connaît pas le manifeste
2. **Manifeste JSON** : sans dépendance, mais mêmes défauts de synchronisation, et moins lisible à la main
3. **En-tête dans chaque document** (*front matter*) : quelques lignes `clé: valeur` entre deux `---`
   - ✅ Un document se suffit à lui-même ; l'import par l'API fonctionne tel quel
   - ✅ Aucune dépendance : un format volontairement minimal, lu en quelques lignes
   - ❌ Pas de vue d'ensemble en un fichier (le catalogue la donne)

Pour l'identité : préfixer par le sujet les clés des thèmes et des cartes, ou garder les identifiants Java tels quels
et ne préfixer que les nouveaux sujets (cas particulier permanent, refusé).

## Décision

- **En-tête de document** (option 3) : `subject`, `title`, `category`, `position`, `status`, toutes obligatoires.
  - Une clé ou une valeur inconnue rend le document invalide : 422 `content.invalid-source`, et échec de la CI.
  - Le fichier s'appelle `<slug>-entretien-reference.md`.
- **Identité** : `subjectId = uuid(slug)`, `themeId = uuid(slug/chapitre)`, `cardId = uuid(slug/chapitre|question
  normalisée)`. Deux sujets peuvent avoir le même chapitre et la même question sans collision.
- **Slugs de thèmes** : préfixés par le sujet, sauf si le titre commence déjà par lui (`java-fondamentaux`,
  `go-glossaire-des-sigles-et-termes`). Ils restent uniques dans tout le catalogue, et les URL actuelles ne changent pas
  de forme.
- **Import par sujet** : la comparaison (et donc les retraits) ne porte que sur les thèmes et les cartes du sujet
  importé. Au démarrage, chaque document est importé dans sa propre transaction : un document invalide n'empêche pas
  d'importer les autres.
- **Brouillons** : un sujet `draft` est importé mais invisible (catalogue public et révisions). L'écran admin pour les
  relire viendra plus tard (phase 1 bis, étape 5).
- **Questions pièges** : le chapitre est reconnu à son titre (« QU'AFFICHE CE CODE ? »), plus à son numéro.
- **Migration** : V6 crée `subject` et rattache chaque thème à un sujet. Le contenu importé avant (identifiants sans
  sujet) est supprimé puis réimporté. C'est possible parce que rien n'a encore été déployé ; c'est aussi pourquoi cette
  étape précède le premier déploiement.

## Conséquences

- ➕ Ajouter un sujet ne demande aucun code : un document, une pull request. Le contrôle de contenu de la CI
  (`ReferenceDocumentTest`) vérifie automatiquement chaque nouveau document.
- ➕ Go (747 cartes) et Java (1 311 cartes) coexistent sans interférence ; réimporter l'un ne touche pas l'autre.
- ➖ Changer le slug d'un sujet change l'identité de toutes ses cartes : le slug est définitif, comme indiqué dans
  CLAUDE.md (section 5.4).
- ➖ La progression enregistrée en développement avant V6 est orpheline (cartes inconnues, ignorées). Aucune donnée de
  production n'est concernée.
- 🔁 Si l'en-tête doit un jour porter des structures (listes, objets), passer à un vrai parseur YAML, par un nouvel ADR.
