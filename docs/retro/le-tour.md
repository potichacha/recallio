# Rétrospective : le Tour (phase 3 quater)

- Période : du 2026-11-04 au 2026-11-05
- Livré : les étapes 1 à 4 dans les PR #70, #72, #73 et #74, la validation et la reprise dans les PR #76 et #77, puis
  les étapes 5 à 10 dans la PR « finir le projet »

## Ce qui a été produit

| | Notions | Pages | « À toi » |
|---|---|---|---|
| Tour de Go | 81 | 81 | 29 |
| Tour de Java | 106 | 106 | 19 |
| Programmer de zéro (en brouillon) | 23 | 23 | 19 |
| **Total** | **210** | **210** | **67** |

Les fonctionnalités :
- le mode « programme » du runner, et le bac à sable qui exécute les pages ;
- une notion validée par 2 ou 3 de ses questions, corrigées par le serveur, sans passer par FSRS ;
- la reprise à la dernière page ouverte, d'un appareil à l'autre ;
- « S'entraîner » : toutes les cartes d'une notion, sans rien enregistrer (ADR 0038) ;
- l'« À toi » jugé sur le serveur, qui garde la sortie attendue pour lui ;
- les exercices de fin de chapitre.

Chaque page est jouée dans le bac à sable par `TourTest` : 344 exécutions, en moins de deux minutes.

## Ce qui a bien marché

- **Le contenu a été écrit en données, puis généré.** Un script (hors dépôt) a exécuté chaque programme deux fois avec
  l'outil du langage, pris la sortie de l'exécution elle-même, formaté le code (gofmt) et inséré le bloc `tour` dans
  la notion. Aucune sortie n'a été recopiée à la main : le texte ne peut pas mentir sur ce qu'affiche le programme.
- **Le bac à sable comme juge final.** Les écarts entre la machine de rédaction et le bac à sable ont tous été attrapés
  avant le merge : encodage de la sortie, locale, version de Go.
- **La règle des deux cartes corrigées par le serveur**, vérifiée par le parseur, garantit que chaque page du Tour
  peut être validée.
- **Les fichiers source compacts de Java 25** ont rendu les pages Java aussi courtes que celles de Go, et ont décidé du
  langage de « Programmer de zéro » (ADR 0039).

## Les pièges, et ce qu'on change

- **La locale décide du séparateur décimal.** `String.format("%.2f")` écrit `4,00` sous Windows en français, et `4.00`
  dans le bac à sable. Le script exécute désormais la JVM en `en_US`, comme le bac à sable. Dans les pages, on précise
  la locale dès que le texte est destiné à un lecteur.
- **L'import automatique du module `java.base`** rend certains noms ambigus dans un fichier compact : `Attributes`
  existe dans `java.util.jar` et dans `java.lang.classfile`. Un import explicite du type lève l'ambiguïté.
- **Une sortie qui dépend du hasard ou de la machine** est interdite : ordre d'une `map` en Go, `HashMap` ou
  `HashSet` affichés en Java, nombre de processeurs, `randomUUID`, date du jour. Les pages trient, ou affichent ce qui ne
  varie pas.
- **Un « À toi » ne peut pas partir d'un code qui ne compile pas**, car `TourTest` exige que le code de départ s'exécute.
  Les erreurs de compilation se montrent donc dans les « Essaie : » du texte, pas dans les « À toi ».

## À faire ensuite

- Relire « Programmer de zéro » dans l'application, puis le publier (une ligne à changer dans `subject.yaml`).
- Enrichir les cartes de « Programmer de zéro » jusqu'à la matrice de l'ADR 0025.
- Des pages plus longues pour certaines notions, découpées en deux ou trois pages ; le format le permet déjà.
