---
subject: algorithmes
title: Algorithmes & complexité
category: fundamentals
position: 190
status: published
---

# Algorithmes & complexité : le document de référence pour les entretiens techniques

> Pour préparer un entretien : algorithmes, structures de données et complexité.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. ALGORITHMES & COMPLEXITÉ
═══════════════════════════════════════════

### Vue d'ensemble

En entretien backend, on teste rarement des algorithmes exotiques. Ce qu'on veut vérifier, c'est que tu sais **raisonner en complexité** (reconnaître qu'une boucle imbriquée sur un gros volume est un problème) et que tu connais les quelques patterns qui résolvent 80% des exercices courants.

La complexité prime sur la micro-optimisation : passer de O(n²) à O(n) sur 100 000 éléments gagne un facteur plusieurs milliers, là où optimiser des constantes gagne quelques pourcents.

### Cours

**Big O** : évolution du coût selon la taille d'entrée, dans le pire cas :
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ) < O(n!)

**Complexités usuelles :**
| Structure | Accès | Recherche | Insertion | Suppression |
|---|---|---|---|---|
| Tableau / ArrayList | O(1) | O(n) | O(n) | O(n) |
| Liste chaînée | O(n) | O(n) | O(1)* | O(1)* |
| HashMap / HashSet |, | O(1) | O(1) | O(1) |
| TreeMap / TreeSet |, | O(log n) | O(log n) | O(log n) |
| Tas (heap) |, | O(n) | O(log n) | O(log n) |

\* si la position est déjà connue.

**Tris** : Java utilise **TimSort** pour les objets (fusion de merge sort et insertion sort, O(n log n), stable) et un quicksort à double pivot pour les primitifs.

**Parcours de graphes/arbres** : **BFS** (en largeur, avec une file, trouve le plus court chemin en nombre d'arêtes) et **DFS** (en profondeur, avec une pile ou la récursion, exploration exhaustive, détection de cycles).

**Patterns de résolution récurrents :**
- **HashMap pour la recherche** : transformer une recherche O(n) en O(1). Résout "two sum", détection de doublons, comptage d'occurrences.
- **Two pointers** : deux index sur une structure triée, souvent pour trouver une paire ou inverser en place. O(n).
- **Sliding window** : fenêtre glissante pour les problèmes de sous-séquence contiguë (plus longue sous-chaîne sans répétition, somme maximale). Évite de tout recalculer.
- **Programmation dynamique** : décomposer en sous-problèmes qui se chevauchent et mémoriser les résultats. Fibonacci, sac à dos, plus longue sous-séquence commune.
- **Récursion + backtracking** : explorer des combinaisons en revenant en arrière (permutations, sudoku).

**Réflexes en live coding :**
1. **Reformuler** l'énoncé pour vérifier la compréhension.
2. **Demander les contraintes** : taille des données, valeurs possibles, cas limites.
3. **Proposer une solution naïve** d'abord, puis l'optimiser, mieux vaut une solution qui marche qu'une élégante inachevée.
4. **Penser à voix haute** : c'est le raisonnement qui est évalué.
5. **Traiter les cas limites** : entrée vide, un seul élément, valeurs nulles, débordement.
6. **Annoncer la complexité** finale, en temps et en espace.

**Notations** : O (borne supérieure, pire cas), Ω (borne inférieure), Θ (borne exacte) ; complexité **amortie** (l'ajout dans une ArrayList est O(1) amorti malgré des redimensionnements en O(n)) ; complexité **spatiale** (mémoire supplémentaire, y compris la pile de récursion).

**Algorithmes de tri** :
| Tri | Moyen | Pire | Mémoire | Stable | Remarque |
|---|---|---|---|---|---|
| Bulle, sélection | O(n²) | O(n²) | O(1) | bulle oui, sélection non | pédagogique |
| Insertion | O(n²) | O(n²) | O(1) | oui | très bon sur des données presque triées ou petites |
| Fusion (merge sort) | O(n log n) | O(n log n) | O(n) | oui | diviser pour régner |
| Rapide (quicksort) | O(n log n) | O(n²) | O(log n) | non | très rapide en pratique, pivot crucial |
| Tas (heapsort) | O(n log n) | O(n log n) | O(1) | non | |
| Comptage, radix | O(n + k) | O(n + k) | O(n + k) | oui | entiers dans une plage bornée |

Un tri **stable** conserve l'ordre relatif des éléments égaux (important pour trier successivement par plusieurs critères). Un tri par comparaison ne peut pas faire mieux que O(n log n).

**Recherche dichotomique** : O(log n) sur un tableau trié ; `Arrays.binarySearch`, `Collections.binarySearch` (renvoient `-(point d'insertion) - 1` si absent). Calculer le milieu par `lo + (hi - lo) / 2` pour éviter le débordement de `(lo + hi) / 2`.

**Arbres** : arbre binaire de recherche (gauche < nœud < droite, O(log n) si équilibré, O(n) s'il dégénère en liste), arbres **équilibrés** (rouge-noir, utilisé par TreeMap ; AVL), parcours **préfixe** (nœud, gauche, droite), **infixe** (gauche, nœud, droite : donne l'ordre trié d'un BST), **postfixe**, **en largeur** (par niveaux, avec une file). B-tree et B+tree : arbres à grand degré, base des index de bases de données.

**Tas et PriorityQueue** : tas binaire, accès au minimum en O(1), insertion et extraction en O(log n), construction en O(n). Pattern **top K** : un tas de taille K (min-heap pour les K plus grands), complexité O(n log K).

**Graphes** : représentation par **liste d'adjacence** (`Map<Noeud, List<Noeud>>`, économe pour les graphes peu denses) ou **matrice d'adjacence** (accès O(1), mémoire O(V²)). Algorithmes : BFS/DFS en O(V + E), **Dijkstra** (plus court chemin à poids positifs, avec une PriorityQueue, O((V + E) log V)), Bellman-Ford (poids négatifs), **tri topologique** (ordonner des tâches selon leurs dépendances, d'un graphe orienté acyclique : résolution de dépendances Maven, ordre de démarrage), **union-find** (composantes connexes), détection de cycle.

**Autres structures** : pile (`ArrayDeque`, appels de fonctions, parenthèses), file (BFS, producteur-consommateur), **trie** (arbre de préfixes, autocomplétion), table de hachage, liste chaînée, matrice, bitset.

**Techniques** : diviser pour régner, glouton (choix localement optimal, correct seulement pour certains problèmes), programmation dynamique (**mémoïsation** descendante ou **tabulation** ascendante), backtracking, deux pointeurs, fenêtre glissante, sommes préfixes (somme d'un intervalle en O(1) après un pré-calcul O(n)), manipulation de bits (`x & (x - 1)` retire le bit de poids faible, XOR pour trouver l'élément unique).

**Récursion** : chaque appel consomme une frame de pile ; une récursion profonde (des dizaines de milliers de niveaux) provoque un `StackOverflowError`. Java n'optimise pas la récursion terminale : sur de grandes profondeurs, on écrit une version itérative avec une pile explicite.

### En situation

- **"Ce traitement est très lent"** → identifier la complexité avant d'optimiser.
- **"Écris une fonction qui..."** → méthode de live coding.
- **"Comment tu détectes des doublons ?"** → HashSet.

### Questions techniques

**Q : Classe les complexités.** R : O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ) < O(n!).

**Q : Complexité d'une recherche en HashMap vs TreeMap ?** R : O(1) en moyenne vs O(log n).

**Q : Quel tri utilise Java ?** R : TimSort pour les objets, quicksort double pivot pour les primitifs.

**Q : BFS vs DFS ?** R : Largeur avec une file (plus court chemin) vs profondeur avec une pile (exploration exhaustive).

**Q : C'est quoi la programmation dynamique ?** R : Décomposer en sous-problèmes qui se chevauchent et mémoriser les résultats.

**Q : Pattern sliding window ?** R : Une fenêtre glissante pour éviter de recalculer sur les sous-séquences contiguës.

**Q : O vs Θ vs Ω ?** R : Borne supérieure vs borne exacte vs borne inférieure.

**Q : C'est quoi la complexité amortie ?** R : Le coût moyen d'une opération sur une longue séquence, malgré des opérations ponctuellement coûteuses.

**Q : C'est quoi un tri stable ?** R : Un tri qui conserve l'ordre relatif des éléments égaux.

**Q : Pire cas du quicksort et comment l'éviter ?** R : O(n²) avec un mauvais pivot ; pivot aléatoire ou médiane de trois, ou double pivot.

**Q : Pourquoi le merge sort est-il utilisé pour les objets ?** R : Il est stable et garanti en O(n log n) (TimSort en est une variante adaptative).

**Q : Limite théorique d'un tri par comparaison ?** R : O(n log n).

**Q : Pourquoi écrire `lo + (hi - lo) / 2` ?** R : Pour éviter le débordement d'entier de `lo + hi`.

**Q : Quel parcours d'un BST donne l'ordre trié ?** R : Le parcours infixe.

**Q : Comment trouver les K plus grands éléments efficacement ?** R : Un min-heap de taille K, en O(n log K).

**Q : Liste vs matrice d'adjacence ?** R : Économe pour les graphes peu denses vs accès O(1) mais mémoire O(V²).

**Q : Quel algorithme pour le plus court chemin pondéré ?** R : Dijkstra pour des poids positifs, Bellman-Ford sinon.

**Q : À quoi sert un tri topologique ?** R : Ordonner des éléments selon leurs dépendances dans un graphe orienté acyclique.

**Q : Mémoïsation vs tabulation ?** R : Récursion avec cache vs remplissage itératif d'un tableau de sous-résultats.

**Q : Java optimise-t-il la récursion terminale ?** R : Non ; une récursion très profonde finit en StackOverflowError.

**Q : C'est quoi un trie ?** R : Un arbre de préfixes, idéal pour l'autocomplétion et la recherche par préfixe.

### Questions de situation

**Q : "Ce traitement met 10 minutes sur 100 000 éléments."**
R : "Je regarderais la **complexité algorithmique** avant toute micro-optimisation. Dix minutes sur 100 000 éléments évoque du quadratique, typiquement une boucle imbriquée, ou une recherche linéaire à l'intérieur d'une boucle.
Le réflexe : identifier la structure utilisée pour les recherches. Passer d'une List à un HashSet fait tomber la recherche de O(n) à O(1), et le traitement global de O(n²) à O(n), un facteur de plusieurs milliers à ce volume.
Je vérifierais aussi qu'il n'y a pas d'appel réseau ou de requête base **dans** la boucle, un N+1 déguisé. Dans ce cas la solution est de charger en une requête, ou de traiter par lots.
Et seulement si l'algorithme est déjà optimal, je regarderais la parallélisation ou le réglage JVM. Optimiser un mauvais algorithme, c'est repeindre une voiture sans moteur."

**Q : "Tu dois ordonner le démarrage de 50 microservices qui dépendent les uns des autres."**
R : "C'est un **tri topologique** sur le graphe des dépendances. Algorithme de Kahn : compter pour chaque service le nombre de dépendances non satisfaites, mettre dans une file ceux qui en ont zéro, et à chaque fois qu'un service est démarré, décrémenter le compteur de ceux qui dépendent de lui. Complexité O(V + E). S'il reste des services à la fin, il existe un **cycle**, qu'il faut signaler.
Mais en pratique je questionnerais le besoin : des services qui doivent démarrer dans un ordre strict sont fragiles. Mieux vaut que chacun démarre indépendamment et attende ses dépendances via ses readiness probes et des retries."
