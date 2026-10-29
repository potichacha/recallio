# 0007. Retour à Java 25

- Statut : accepté (remplace l'ADR 0006)
- Date : 2026-10-29

## Contexte

L'ADR 0006 avait retenu Java 21, surtout parce que Java 21 était déjà installé et pas
Java 25. Avant de créer le projet Quarkus, on constate que le poste dispose déjà de
**Java 25** (Temurin et GraalVM), et qu'il est même le JDK actif (`JAVA_HOME`).

L'argument principal de l'ADR 0006 ne tient donc plus. Au même moment, garder Java 21
aurait demandé de reconfigurer l'environnement local pour l'aligner sur la CI.

## Options envisagées

1. **Garder Java 21** (ADR 0006)
   - ✅ Outillage le plus éprouvé
   - ❌ Il faut reconfigurer `JAVA_HOME` pour ce projet, sinon le local et la CI divergent
   - ❌ Pas d'accès aux fonctionnalités 22 à 25 ; la migration arrive plus tôt
2. **Revenir à Java 25** (choix initial de l'ADR 0002)
   - ✅ Déjà installé et actif : aucun coût
   - ✅ `_`, Stream Gatherers, constructeurs flexibles, virtual threads sans pinning (JEP 491)
   - ✅ Support plus long
   - ❌ Outillage récent à surveiller (Error Prone, plugins Maven)

## Décision

On revient à **Java 25 (LTS)** : `maven.compiler.release=25`, Temurin 25 en CI et
dans l'image Docker. L'ADR 0006 passe au statut « remplacé par 0007 ».

## Conséquences

- ➕ L'environnement local actuel convient tel quel, sans reconfiguration.
- ➕ Les fonctionnalités de Java 22 à 25 sont disponibles.
- ➖ Il faut choisir des versions récentes d'Error Prone et des plugins Maven compatibles
  avec Java 25 (vérification à l'étape 3).
- ➖ En local, `JAVA_HOME` pointe sur GraalVM 25 alors que la CI utilisera Temurin 25. C'est
  sans impact en mode JVM ; en cas de comportement différent, aligner le local sur Temurin.
- 🔁 Leçon retenue : vérifier l'environnement (`java -version`, `mvn -v`) avant de rédiger
  un ADR qui s'appuie dessus.
