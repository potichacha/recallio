# 0008. Identifiants des tables métier : UUID v7

- Statut : accepté
- Date : 2026-10-29

## Contexte

Il faut choisir le type des clés primaires avant la première migration métier
(CLAUDE.md, section 10, point 3). Les identifiants apparaissent dans les URL de l'API
(`/api/v1/cards/{id}`), sont générés aussi bien côté serveur que pour des imports, et la
phase 4 prévoit plusieurs organisations dans la même base.

## Options envisagées

1. **Séquences (`bigint`)**
   - ✅ Compactes (8 octets), lisibles, index très efficaces
   - ❌ Devinables dans les URL : facilite l'énumération des ressources d'autrui
   - ❌ Générées par la base : l'identifiant n'existe qu'après l'insertion
2. **UUID v4 (aléatoires)**
   - ✅ Non devinables, générables partout
   - ❌ Totalement aléatoires : insertions dispersées dans l'index B-tree, ce qui le fragmente
3. **UUID v7 (RFC 9562 : horodatage en tête, puis aléa)**
   - ✅ Non devinables en pratique, générables dans le domaine sans la base
   - ✅ Croissants dans le temps : insertions en fin d'index, comme une séquence
   - ❌ 16 octets ; l'horodatage de création est lisible dans l'identifiant

## Décision

On choisit les **UUID v7**, en colonne PostgreSQL `uuid`.

- Ils sont générés **dans l'application** par un générateur du module `platform`, derrière une
  interface injectée, pour que les tests restent déterministes.
- Exception : la carte a un **identifiant stable** dérivé du chapitre et d'un hash de la question
  (CLAUDE.md, section 5.2). C'est un UUID v8 (« personnalisé », dérivé d'un hash), qui reste
  compatible avec la colonne `uuid`.

## Conséquences

- ➕ Un identifiant d'URL ne permet pas de deviner celui d'un autre utilisateur ; l'autorisation
  côté serveur reste de toute façon obligatoire.
- ➕ Les agrégats peuvent créer leur identifiant avant la persistance.
- ➖ L'horodatage de création est lisible dans l'identifiant : ce n'est pas une donnée sensible
  pour ce produit.
- 🔁 PostgreSQL 18 fournit `uuidv7()`. On pourra s'en servir pour des insertions SQL
  directes, mais l'application reste la source des identifiants.
