# 0009. Import du contenu et identité stable des cartes

- Statut : accepté
- Date : 2026-10-29

## Contexte

Les cartes viennent d'un unique document Markdown (`content/source/java-entretien-reference.md`, environ
1 300 questions) qui sera corrigé et enrichi au fil du temps. La progression des utilisateurs (FSRS, phase 1)
est rattachée aux cartes : une correction de texte ne doit pas la faire perdre. CLAUDE.md (section 5.2) impose
un identifiant dérivé du chapitre et d'un hash normalisé de la question, un import idempotent avec rapport, et
un parseur isolé derrière une interface.

## Options envisagées

1. **Identifiant aléatoire et correspondance par texte exact**
   - ✅ Simple
   - ❌ La moindre correction de la question crée une nouvelle carte et perd la progression
2. **Identifiant explicite dans la source** (par exemple `<!-- id: 42 -->` devant chaque question)
   - ✅ Robuste à toute réécriture
   - ❌ Il faut modifier et maintenir 1 300 annotations à la main ; un oubli ou un copier-coller crée des doublons
3. **Identifiant dérivé du thème et de la question normalisée** (UUID v8, ADR 0008)
   - ✅ Aucune annotation dans la source ; le même texte donne toujours le même identifiant
   - ✅ Robuste aux corrections de forme (casse, accents, ponctuation, gras, code)
   - ❌ Réécrire le *sens* d'une question crée une nouvelle carte (et retire l'ancienne)

## Décision

On choisit l'option 3.

- **Normalisation** : décomposition Unicode et suppression des accents, minuscules, suppression des marqueurs
  Markdown (`*`, `` ` ``, `_`), et toute ponctuation devient un séparateur. Les **opérateurs**
  (`< > = + - / % & | ^ ~ !`) sont conservés : sans eux, `` `>>` vs `>>>` `` et `` `&` vs `&&` `` se
  réduiraient tous deux à « vs ». Un test vérifie que les 1 311 questions du document ont des identifiants distincts.
- **Versions** : l'empreinte SHA-256 du texte exact (type, recto, verso) détecte tout changement ; un changement
  crée une nouvelle ligne `card_version`, et la carte garde son identifiant. Les versions sont immuables.
- **Retrait** : une carte absente de la source est marquée `retired_at`, jamais supprimée ; elle est réactivée si
  elle réapparaît.
- **Idempotence** : le planificateur d'import (`ImportPlanner`, code pur) compare la source au catalogue et produit
  un plan ; un plan vide signifie que rien n'a changé. L'import s'exécute en une transaction.
- **Rapport** : cartes ajoutées, modifiées, inchangées, retirées et réactivées ; lignes non reconnues ; questions
  en double (seule la première est gardée).
- **Déclenchement** : au démarrage en dev et dans le compose local (`recallio.content.import-on-startup`), et par
  `POST /api/v1/admin/content-imports` (rôle `admin`) ailleurs.
- **Isolation** : le format Markdown est lu par `MarkdownReferenceParser`, derrière le port `ContentParser`. Changer
  de format de source (un fichier par carte, par exemple) ne touchera ni l'import ni la base.
- **Types de cartes** : questions techniques → `FLASHCARD`, sauf au chapitre 14 → `CODE_OUTPUT` ; questions de
  situation → `OPEN_SCENARIO` ; glossaire → `DEFINITION`.

## Conséquences

- ➕ Corriger une faute, reformuler la réponse ou ajouter du gras ne fait perdre aucune progression.
- ➕ Le contenu est importable autant de fois qu'on veut, et le rapport signale les erreurs de format.
- ➖ Reformuler en profondeur une question crée une nouvelle carte : la progression sur l'ancienne n'est pas
  transférée. C'est acceptable pour la V1 ; un mécanisme d'alias (« cette carte remplace telle autre ») pourra
  être ajouté si le besoin apparaît.
- ➖ Deux questions identiques au sens de la normalisation, dans un même thème, ne peuvent pas coexister : le rapport
  le signale.
- 📝 Écart avec l'ébauche de modèle (CLAUDE.md, section 7) : la colonne `question` s'appelle `prompt` (le recto
  n'est pas toujours une question, par exemple le terme d'une définition), et la colonne `payload` n'est pas encore
  créée. Elle arrivera avec les QCM (phase 2), par une nouvelle migration.
