# 0026. Le dossier d'un sujet : un fichier YAML par notion, des cartes à clé explicite

- Statut : accepté
- Date : 2026-11-02

## Contexte

La refonte du contenu (ADR 0025) fait de la **notion** l'unité de contenu : chaque notion du référentiel porte une
courte leçon et des cartes de formats variés. Le document Markdown d'un sujet ne permet pas de rattacher une carte à
une notion, et l'identité de ses cartes dérive du texte de la question (ADR 0009) : reformuler une question en fait une
autre carte. Il faut un format de source par notion, avant de migrer les cartes existantes et d'ajouter les nouveaux
formats (vrai ou faux, compléter le code, bug à corriger, exercices).

## Options envisagées

Pour le format d'un fichier de notion :

1. **Markdown avec un en-tête** : agréable à écrire, mais chaque format demande une syntaxe à inventer et un parseur à
   maintenir, la partie la plus fragile de l'import aujourd'hui.
2. **YAML** (retenu) : lu par Jackson, déjà présent ; une propriété inconnue ou manquante fait échouer la lecture ; un
   nouveau format n'est qu'un nouveau champ. Le Markdown des textes et le code tiennent dans des blocs `|`.

Pour l'identité d'une carte :

1. Garder le hash de la question normalisée : reformuler fait perdre la progression.
2. **Une clé explicite, lisible, unique dans la notion** (retenu), par exemple `concatenation-a-la-compilation`.
3. Un UUID écrit dans le fichier : stable, mais illisible en relecture et pénible à rédiger.

## Décision

**Le dossier d'un sujet**, `content/<sujet>/` :

```
content/java/
├── subject.yaml                       # slug, titre, catégorie, position, statut (les clés de l'ADR 0013)
├── referentiel/06-collections.yaml    # le référentiel (ADR 0025)
└── notions/collections/hashset.yaml   # une notion : notions/<domaine>/<notion>.yaml
```

**Un fichier de notion** : l'identifiant de la notion, une leçon facultative (elle deviendra obligatoire avec les
contrôles de la matrice), et des cartes `key`, `format`, `question`, `answer`, plus `choices` pour un QCM. Les formats
déjà pris en charge sont `course`, `multiple-choice`, `code-output`, `scenario` et `definition` ; les autres sont
refusés tant que l'application ne sait pas les présenter.

**Identité.** Une notion est identifiée par son sujet et son identifiant ; une carte par son sujet, sa notion et sa
clé. Corriger ou reformuler le texte crée une nouvelle version de la même carte ; changer la clé crée une autre carte.

**Thèmes.** Un domaine du référentiel qui a au moins une carte devient un thème, de slug `<sujet>-<domaine>` : il ne
change pas quand on retouche le titre du domaine, et les réglages de groupe qui le citent restent valides.

**Notions en base.** Une table `notion` garde tout le référentiel (avec ou sans cartes), et chaque carte sa notion
(`card.notion_id`, vide pour un document Markdown). Les parcours et la maîtrise par notion s'appuieront dessus.

**Retraits.** Un thème ou une notion qui disparaît de sa source est **retiré**, comme une carte : il n'est plus listé
et un thème retiré libère son slug (index unique partiel). Cela corrige aussi les thèmes vides laissés par le
découpage du document Java.

**Coexistence.** Chaque sujet a une seule source : un document Markdown ou un dossier. Les deux formats sont importés
au démarrage ; la CI vérifie chaque dossier (`SubjectFolderTest`) comme chaque document.

## Conséquences

- `jackson-dataformat-yaml` passe de la portée test à la portée runtime (version gérée par le BOM Quarkus).
- Un dossier ne s'importe pas par l'endpoint d'administration, qui reçoit un seul document ; l'import en production des
  dossiers se décidera avec le déploiement (contenu embarqué dans l'image, ou archive envoyée par un admin).
- La migration du contenu Java vers ce format change l'identité de ses cartes : aucun utilisateur réel n'a encore de
  progression (ADR 0025).
