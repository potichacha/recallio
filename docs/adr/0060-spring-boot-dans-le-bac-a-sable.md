# 0060. Spring Boot dans le bac à sable Java

- Statut : accepté
- Date : 2026-11-16
- Prolonge : ADR 0029 (le format des exercices), ADR 0030 (le bac à sable), ADR 0033 (un runner multi-langage), ADR
  0037 (les programmes du Tour)

## Contexte

L'étape 7.2 de la feuille de route demande des sujets Spring Boot, puis Quarkus, « prêts à l'emploi » : un référentiel,
des leçons, des cartes à choix, et des exercices jugés par `@SpringBootTest` dans un bac à sable qui embarque le
framework. Le bac à sable Java ne connaît que le JDK et JUnit.

Un exercice Spring Boot s'écrit en Java : des fichiers `.java`, comme ceux du sujet Java. Le runner choisit l'image d'un
exercice d'après l'extension de ses fichiers (ADR 0033), et le backend fait de même.

Deux mesures guident la décision, dans un conteneur limité comme en production (un processeur, 512 Mo, réseau coupé) :

- un exercice `@SpringBootTest`, `@WebMvcTest` avec MockMvc ou `@DataJpaTest` sur H2 se juge en 6 à 10 secondes,
  compilation comprise, sous le délai de 20 secondes du runner ;
- mettre les bibliothèques de Spring Boot sur le classpath de tout le Java coûte une demi-seconde à chaque programme du
  Tour (2,0 secondes au lieu de 1,5 pour lancer un `Bonjour`, conteneur compris).

## Options envisagées

- **Une image `sandbox/spring` et un nouveau langage `SPRING_BOOT`** : l'image Java reste inchangée. Mais le langage ne
  se déduit plus de l'extension. Il faudrait le déclarer (dans `subject.yaml` ou `exercise.yaml`), et le porter dans le
  backend, l'API du runner, l'OpenAPI et le front, pour du code qui reste du Java.
- **Les bibliothèques dans l'image Java** (retenu) : un exercice Spring Boot est un exercice Java dont le code importe
  Spring. Rien ne change dans le backend, le runner, l'API ni le front.

## Décision

- L'image `sandbox/java` embarque Spring Boot (4.1) dans `/opt/recallio/lib` : `spring-boot-starter-webmvc`,
  `-validation`, `-data-jpa` avec H2, et leurs starters de test (`-test`, `-webmvc-test`, `-data-jpa-test`), donc
  AssertJ et Mockito. Un projet Maven qui ne se construit pas, `sandbox/java/libraries/pom.xml`, en fixe les versions ;
  une étape de construction les télécharge en vérifiant leurs empreintes, et Dependabot les tient à jour.
- JUnit reste celui du lanceur autonome de l'image : le JUnit de Spring Boot, et ce qu'il embarque, sont écartés, pour
  qu'il n'y en ait qu'une copie.
- **Les bibliothèques ne servent qu'au code qui les importe.** Si le code ou ses tests importent autre chose que le JDK et
  JUnit, `run.sh` les ajoute au classpath de `javac` et de la JVM ; sinon, le Java ordinaire compile et s'exécute comme
  avant, sans la demi-seconde de plus.
- `javac` compile avec `-parameters`, comme les plugins Maven et Gradle de Spring Boot : Spring lit le nom des
  paramètres d'un contrôleur (`@RequestParam int base`).
- La JVM garde son tas de 256 Mo : un contexte Spring avec JPA y tient.
- Un exercice Spring Boot met son code dans un package (`boutique/...`) avec une classe `@SpringBootApplication` :
  dans le package par défaut, Spring parcourrait tout le classpath.

## Conséquences

- `SpringBootSandboxTest` juge un `@SpringBootTest`, un `@WebMvcTest` avec un `@MockitoBean`, un `@DataJpaTest` et un
  programme du Tour qui importe Spring, avec le délai de la production. Il vérifie aussi qu'un programme sans import n'a
  pas les bibliothèques.
- L'image Java passe de 554 Mo à 820 Mo (166 Mo de bibliothèques). Sa construction télécharge ces bibliothèques en 45
  secondes environ : les jobs `runner` et `e2e` de la CI, qui la construisent sans cache, s'allongent d'autant.
- Les exercices Java peuvent aussi utiliser AssertJ et Mockito, s'ils les importent.
- Quarkus n'est pas réglé par cette décision : `@QuarkusTest` a besoin du modèle de l'application que construit Maven ou
  Gradle, et se décidera dans son propre ADR.
