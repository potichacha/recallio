# 0004. Error Prone et NullAway dès la phase 0

- Statut : accepté
- Date : 2026-10-20

## Contexte

Les `NullPointerException` sont l'une des causes de bugs les plus fréquentes en
Java. Le système de types ne distingue pas une référence qui peut être nulle d'une
référence qui ne peut pas l'être.

Error Prone est un compilateur enrichi d'analyses statiques. NullAway en est une
extension qui vérifie la nullabilité à la compilation, à partir des annotations
standard JSpecify (`@NullMarked`, `@Nullable`).

## Options envisagées

1. **Ne pas utiliser d'analyse de nullabilité**
   - ✅ Aucune configuration
   - ❌ Les NPE sont découvertes à l'exécution
2. **Activer plus tard**, quand le code existe
   - ✅ Démarrage plus léger
   - ❌ Des centaines d'avertissements à corriger d'un coup ; en pratique, souvent jamais fait
3. **Activer dès la phase 0**, sur une base de code vide
   - ✅ Chaque nouvelle classe respecte la règle dès sa création
   - ✅ La nullabilité devient un contrat lisible dans les signatures
   - ❌ Configuration Maven plus technique, à maintenir lors des montées de version

## Décision

On choisit l'option 3.

- Error Prone et NullAway sont branchés sur `maven-compiler-plugin` ; une erreur
  NullAway fait échouer la compilation.
- Les packages `com.recallio` sont annotés `@NullMarked` (via `package-info.java`) :
  tout est non nul par défaut, `@Nullable` est explicite.
- Le code généré (par Quarkus ou d'autres outils) est exclu de l'analyse.
- `Optional` reste réservé aux types de retour (CLAUDE.md, section 4.3).

## Conséquences

- ➕ Une catégorie entière de bugs est éliminée à la compilation.
- ➕ Les signatures documentent ce qui peut être absent.
- ➖ Les flags `--add-exports` et les versions d'Error Prone doivent suivre les
  versions du JDK : une montée de Java peut casser le build.
- ➖ Certaines API de frameworks (injection, JPA) peuvent demander des
  annotations ou des exclusions ciblées.
- 🔁 Si la configuration devient un frein récurrent, on pourra passer NullAway en
  avertissement plutôt qu'en erreur, via un nouvel ADR.
