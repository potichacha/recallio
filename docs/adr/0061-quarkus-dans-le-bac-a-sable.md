# 0061. Quarkus dans le bac à sable Java

- Statut : accepté
- Date : 2026-11-16
- Prolonge : ADR 0060 (Spring Boot dans le bac à sable Java)
- Révise : ADR 0030 (le délai d'une exécution passe de 20 à 30 secondes)

## Contexte

L'étape 7.2 de la feuille de route demande un sujet Quarkus « prêt à l'emploi », avec des exercices jugés par
`@QuarkusTest`. L'ADR 0060 a mis Spring Boot dans le bac à sable Java en ajoutant ses bibliothèques au classpath. Ce
n'est pas suffisant pour Quarkus. `@QuarkusTest` construit l'application au démarrage des tests (l'augmentation) à
partir du modèle du projet : il lit le `pom.xml`, résout chaque extension et son artefact `-deployment` dans un dépôt
Maven, puis génère le code de l'application. Sans projet ni dépôt, il ne démarre pas.

Les mesures ont été faites dans un conteneur limité comme en production : un processeur, 512 Mo, réseau coupé. Avec un
exercice réaliste (une entité Panache sur H2, une ressource REST validée, trois tests RestAssured) :

- **Maven dans le conteneur** (`mvn -o test`) : la JVM de Maven et celle de Surefire ne tiennent pas ensemble dans
  512 Mo, et le conteneur est tué faute de mémoire au bout de 23 à 28 secondes. Sans processus séparé
  (`forkCount=0`), `@QuarkusTest` échoue au démarrage.
- **Comme un IDE** : `javac` compile le code et les tests là où Maven les mettrait (`target/classes`,
  `target/test-classes`), puis le lanceur JUnit les exécute dans une seule JVM, avec le `pom.xml` à la racine du projet
  et un dépôt Maven préparé, hors ligne. Les tests passent en 15 à 18 secondes : 4 pour compiler, 11 à 12 pour que
  Quarkus construise et démarre l'application, le reste pour les tests.

Le runner tue une exécution au bout de 20 secondes (ADR 0030), ce qui laisse moins de 3 secondes de marge, et aucune
sur une machine plus lente.

## Options envisagées

- **Maven dans le conteneur** : écartée, puisque Maven ne tient pas dans la mémoire du bac à sable.
- **Des exercices sans `@QuarkusTest`**, qui instancient les classes à la main : écartée. On n'y verrait ni CDI, ni
  REST, ni Panache, alors que c'est ce qu'un entretien Quarkus interroge.
- **Une image `sandbox/quarkus` et un langage `QUARKUS`** : écartée pour les raisons de l'ADR 0060. Le langage ne se
  déduit plus de l'extension, et il faudrait le porter dans le backend, l'API, l'OpenAPI et le front, pour du code qui
  reste du Java.
- **Un projet Quarkus modèle dans l'image Java, exécuté comme un IDE** (retenu).

## Décision

- L'image `sandbox/java` embarque un **projet Quarkus modèle** dans `/opt/recallio/quarkus`. Il comprend quatre
  éléments.
  - Un `pom.xml` (`sandbox/java/quarkus/pom.xml`), à la version de Quarkus du backend, avec les extensions
    `quarkus-rest-jackson`, `quarkus-hibernate-validator`, `quarkus-hibernate-orm-panache` et `quarkus-jdbc-h2`, et
    pour les tests `quarkus-junit`, `quarkus-junit-mockito` (`@InjectMock`) et RestAssured.
  - Un `application.properties` commun à tous les exercices : H2 en mémoire, schéma recréé à chaque démarrage, sans
    Dev Services (le bac à sable n'a ni réseau ni Docker), journaux réduits aux avertissements.
  - Un dépôt Maven hors ligne, rempli à la construction de l'image en jugeant une fois, avec Maven, un exercice de
    chauffe (`sandbox/java/quarkus/warmup/`). Il contient tout ce que Quarkus résout au démarrage d'un test.
  - Le classpath des tests, écrit par Maven à la même construction.
- **Seul le code qui importe Quarkus est jugé ainsi.** Si le code ou ses tests importent `io.quarkus`, `run.sh`
  construit le projet dans `/tmp/projet`, compile avec `javac -parameters -proc:none`, et lance les tests dans une
  seule JVM de 300 Mo de tas. Le reste du Java, Spring Boot compris, se juge comme avant. Les programmes du Tour ne
  passent jamais par ce mode.
- **Une erreur du build de Quarkus est une erreur de compilation.** Quarkus valide l'injection en construisant
  l'application. Une dépendance introuvable ou ambiguë fait donc échouer ce build, et aucun test ne s'exécute : le
  rapport de JUnit est vide. `run.sh` rend alors son message (« Unsatisfied dependency for type … »), sans la pile
  d'appels, comme une erreur de `javac`. Le joueur lit ainsi pourquoi rien n'a tourné. Une erreur au démarrage, comme
  une propriété absente, reste un test en erreur.
- **Le délai d'une exécution passe de 20 à 30 secondes** (`RECALLIO_SANDBOX_TIMEOUT`), pour toutes les exécutions.
  C'est une seule valeur, plus simple qu'un délai par mode, et un exercice Quarkus garde dix secondes de marge. Le
  délai de lecture du backend vers le runner passe de 30 à 40 secondes, pour rester au-dessus.

## Conséquences

- `QuarkusSandboxTest` juge, avec le délai de la production, cinq exercices : une ressource REST avec Panache et la
  validation, une assertion qui échoue sur un bean injecté, un bean remplacé par `@InjectMock`, une erreur de
  compilation, et une dépendance introuvable.
- L'image Java grossit de 188 Mo : le projet modèle, ses bibliothèques et leurs artefacts `-deployment`. Sa
  construction dure environ une minute de plus : les jobs `runner` et `e2e` de la CI, qui la construisent sans cache,
  s'allongent d'autant.
- Un joueur attend 15 à 20 secondes le verdict d'un exercice Quarkus, contre moins de 10 pour Spring Boot. En CI,
  `ExercisesTest` juge le code de départ et la solution de chaque exercice : environ 35 secondes par exercice Quarkus,
  seulement quand le sujet Quarkus change (étape 8.10.2).
- Une boucle sans fin occupe un des quatre créneaux du runner 30 secondes au lieu de 20.
- Un exercice Quarkus ne fournit pas son `application.properties`. Sa configuration propre passe par un
  `@TestProfile`, un seul par exercice : un second profil redémarre Quarkus, soit dix secondes de plus. Il n'a ni
  base PostgreSQL, ni Dev Services, ni `@QuarkusIntegrationTest`, ni mode natif.
- Dependabot tient à jour `sandbox/java/quarkus` dans un groupe `quarkus`, comme le backend : les deux versions
  doivent avancer ensemble.
- Le cache AOT du JDK 25 (JEP 483, 514 et 515) pourrait raccourcir le démarrage de Quarkus. Il n'est pas tenté ici :
  il ne couvre pas les chargeurs de classes de Quarkus, et le gain reste à mesurer.
