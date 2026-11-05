---
subject: algorithmes
title: Algorithmes & complexité
category: fundamentals
position: 190
status: draft
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

═══════════════════════════════════════════
# 2. EXERCICES DE LIVE CODING CORRIGÉS
═══════════════════════════════════════════

### Vue d'ensemble

Le *live coding* d'un entretien Java porte rarement sur des algorithmes avancés. On retrouve presque toujours les mêmes familles : manipulation de chaînes, recherche dans un tableau, comptage avec une Map, structures de pile et de liste chaînée, et de plus en plus des exercices de **Streams** sur une liste d'objets métier. Ce chapitre rassemble les exercices les plus fréquents, avec une solution idiomatique et sa complexité. Toutes les solutions ont été compilées et exécutées sur un JDK 21.

Rappel de méthode (voir le chapitre Algorithmes) : reformuler, demander les contraintes et les cas limites, proposer une solution simple, l'améliorer, annoncer la complexité, et **parler en codant**.

### Cours

**Les réflexes qui reviennent dans presque tous les exercices** :
- Rechercher ou compter → `HashMap`/`HashSet` (O(1)).
- Ordre d'apparition à conserver → `LinkedHashMap`/`LinkedHashSet`.
- Parcourir par les deux bouts → deux pointeurs.
- Sous-chaîne ou sous-tableau contigu → fenêtre glissante.
- Appariement, imbrication, retour arrière → pile (`ArrayDeque`).
- K meilleurs éléments → `PriorityQueue` de taille K.
- Tableau trié → recherche dichotomique.
- Cas limites à annoncer : null, vide, un seul élément, doublons, valeurs négatives, débordement.

**1. FizzBuzz** (O(n))
```java
static List<String> fizzBuzz(int n) {
    return IntStream.rangeClosed(1, n)
        .mapToObj(i -> i % 15 == 0 ? "FizzBuzz" : i % 3 == 0 ? "Fizz" : i % 5 == 0 ? "Buzz" : String.valueOf(i))
        .toList();
}
```
Tester 15 avant 3 et 5 est le piège. Une boucle `for` classique est tout aussi acceptable.

**2. Inverser une chaîne** (O(n))
```java
static String inverser(String s) {
    char[] c = s.toCharArray();
    for (int i = 0, j = c.length - 1; i < j; i++, j--) { char t = c[i]; c[i] = c[j]; c[j] = t; }
    return new String(c);
}
```
En une ligne : `new StringBuilder(s).reverse().toString()` (qui gère aussi correctement les paires de substitution Unicode). On demandera souvent la version manuelle.

**3. Palindrome en ignorant la ponctuation et la casse** (O(n), O(1) en mémoire)
```java
static boolean estPalindrome(String s) {
    int i = 0, j = s.length() - 1;
    while (i < j) {
        char a = s.charAt(i), b = s.charAt(j);
        if (!Character.isLetterOrDigit(a)) { i++; continue; }
        if (!Character.isLetterOrDigit(b)) { j--; continue; }
        if (Character.toLowerCase(a) != Character.toLowerCase(b)) return false;
        i++; j--;
    }
    return true;
}
```

**4. Anagrammes** (O(n))
```java
static boolean sontAnagrammes(String a, String b) {
    if (a.length() != b.length()) return false;
    Map<Character, Integer> compte = new HashMap<>();
    for (char c : a.toCharArray()) compte.merge(c, 1, Integer::sum);
    for (char c : b.toCharArray()) {
        Integer n = compte.merge(c, -1, Integer::sum);
        if (n < 0) return false;
    }
    return true;
}
```
Alternative : trier les deux tableaux de caractères et les comparer (O(n log n)). Pour l'alphabet a-z seul, un `int[26]` est encore plus rapide.

**5. Premier caractère non répété** (O(n))
```java
static Optional<Character> premierNonRepete(String s) {
    Map<Character, Long> freq = s.chars().mapToObj(c -> (char) c)
        .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    return freq.entrySet().stream().filter(e -> e.getValue() == 1)
        .map(Map.Entry::getKey).findFirst();
}
```
La `LinkedHashMap` conserve l'ordre d'apparition : c'est le point clé. Pour `"aabbcdc"`, la réponse est `d`.

**6. Compter les mots d'un texte** (O(n))
```java
static Map<String, Long> compterMots(String texte) {
    return Arrays.stream(texte.toLowerCase().split("\\W+"))
        .filter(m -> !m.isBlank())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
}
```
Attention : `\W` ne reconnaît pas les lettres accentuées comme des lettres ; pour du français, `split("[^\\p{L}\\p{N}]+")`.

**7. Fibonacci et factorielle**
```java
static long fibonacci(int n) {              // O(n) en temps, O(1) en mémoire
    if (n < 2) return n;
    long a = 0, b = 1;
    for (int i = 2; i <= n; i++) { long c = a + b; a = b; b = c; }
    return b;
}
static BigInteger factorielle(int n) {      // BigInteger : 21! dépasse déjà un long
    BigInteger r = BigInteger.ONE;
    for (int i = 2; i <= n; i++) r = r.multiply(BigInteger.valueOf(i));
    return r;
}
```
La version récursive naïve de Fibonacci est exponentielle (O(2ⁿ)) : à mentionner, puis corriger par mémoïsation ou par la version itérative. `fibonacci(92)` est le maximum qui tient dans un long.

**8. Nombres premiers**
```java
static boolean estPremier(int n) {                     // O(√n)
    if (n < 2) return false;
    for (int d = 2; (long) d * d <= n; d++) if (n % d == 0) return false;
    return true;
}
static List<Integer> crible(int max) {                 // crible d'Ératosthène, O(n log log n)
    boolean[] compose = new boolean[max + 1];
    List<Integer> premiers = new ArrayList<>();
    for (int i = 2; i <= max; i++) {
        if (compose[i]) continue;
        premiers.add(i);
        for (long j = (long) i * i; j <= max; j += i) compose[(int) j] = true;
    }
    return premiers;
}
```

**9. Two Sum : indices de deux nombres dont la somme vaut une cible** (O(n))
```java
static int[] twoSum(int[] t, int cible) {
    Map<Integer, Integer> vus = new HashMap<>();
    for (int i = 0; i < t.length; i++) {
        Integer j = vus.get(cible - t[i]);
        if (j != null) return new int[]{j, i};
        vus.put(t[i], i);
    }
    return new int[0];
}
```
La solution naïve à deux boucles est O(n²) ; la Map transforme la recherche du complément en O(1). Si le tableau est trié : deux pointeurs, O(1) en mémoire.

**10. Trouver les doublons** (O(n))
```java
static Set<Integer> doublons(int[] t) {
    Set<Integer> vus = new HashSet<>(), doublons = new LinkedHashSet<>();
    for (int x : t) if (!vus.add(x)) doublons.add(x);
    return doublons;
}
```
`Set.add` renvoie `false` si l'élément était déjà présent : c'est l'astuce.

**11. Recherche dichotomique** (O(log n))
```java
static int rechercheDichotomique(int[] t, int x) {
    int lo = 0, hi = t.length - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;          // évite le débordement de lo + hi
        if (t[mid] == x) return mid;
        if (t[mid] < x) lo = mid + 1; else hi = mid - 1;
    }
    return -1;
}
```

**12. Parenthèses équilibrées** (O(n))
```java
static boolean bienParenthese(String s) {
    Deque<Character> pile = new ArrayDeque<>();
    Map<Character, Character> paires = Map.of(')', '(', ']', '[', '}', '{');
    for (char c : s.toCharArray()) {
        if (paires.containsValue(c)) pile.push(c);
        else if (paires.containsKey(c)) {
            if (pile.isEmpty() || pile.pop() != paires.get(c)) return false;
        }
    }
    return pile.isEmpty();
}
```
`"{[()()]}"` → vrai, `"([)]"` → faux, `"(("` → faux (la pile n'est pas vide à la fin, cas souvent oublié).

**13. Inverser une liste chaînée et détecter un cycle**
```java
static class Noeud { int val; Noeud suivant; Noeud(int v) { val = v; } }

static Noeud inverserListe(Noeud tete) {         // O(n), O(1) en mémoire
    Noeud precedent = null, courant = tete;
    while (courant != null) {
        Noeud suivant = courant.suivant;
        courant.suivant = precedent;
        precedent = courant;
        courant = suivant;
    }
    return precedent;
}

static boolean aUnCycle(Noeud tete) {             // algorithme de Floyd (lièvre et tortue)
    Noeud lent = tete, rapide = tete;
    while (rapide != null && rapide.suivant != null) {
        lent = lent.suivant;
        rapide = rapide.suivant.suivant;
        if (lent == rapide) return true;
    }
    return false;
}
```

**14. Fusionner deux tableaux triés** (O(n + m)) : la brique du tri fusion.
```java
static int[] fusionner(int[] a, int[] b) {
    int[] r = new int[a.length + b.length];
    int i = 0, j = 0, k = 0;
    while (i < a.length && j < b.length) r[k++] = a[i] <= b[j] ? a[i++] : b[j++];
    while (i < a.length) r[k++] = a[i++];
    while (j < b.length) r[k++] = b[j++];
    return r;
}
```

**15. Sous-tableau de somme maximale** (algorithme de Kadane, O(n))
```java
static int sommeMax(int[] t) {
    int meilleur = t[0], courant = t[0];
    for (int i = 1; i < t.length; i++) {
        courant = Math.max(t[i], courant + t[i]);   // prolonger ou recommencer
        meilleur = Math.max(meilleur, courant);
    }
    return meilleur;
}
```
Pour `{-2, 1, -3, 4, -1, 2, 1, -5, 4}` : 6 (le sous-tableau `4, -1, 2, 1`).

**16. Plus longue sous-chaîne sans caractère répété** (fenêtre glissante, O(n))
```java
static int plusLongueSansRepetition(String s) {
    Map<Character, Integer> dernierePos = new HashMap<>();
    int debut = 0, max = 0;
    for (int i = 0; i < s.length(); i++) {
        Integer p = dernierePos.put(s.charAt(i), i);
        if (p != null && p >= debut) debut = p + 1;   // on fait avancer le début de la fenêtre
        max = Math.max(max, i - debut + 1);
    }
    return max;
}
```
`"abcabcbb"` → 3, `"pwwkew"` → 3.

**17. Les K éléments les plus fréquents** (O(n log k))
```java
static List<Integer> topKFrequents(int[] t, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : t) freq.merge(x, 1, Integer::sum);
    PriorityQueue<Map.Entry<Integer, Integer>> tas = new PriorityQueue<>(Map.Entry.comparingByValue());
    for (var e : freq.entrySet()) {
        tas.offer(e);
        if (tas.size() > k) tas.poll();              // on garde les k plus fréquents
    }
    List<Integer> r = new ArrayList<>();
    while (!tas.isEmpty()) r.add(tas.poll().getKey());
    Collections.reverse(r);
    return r;
}
```

**18. Nombre manquant de 0 à n** (O(n), O(1))
```java
static int nombreManquant(int[] t) {
    int n = t.length;
    long attendu = (long) n * (n + 1) / 2;
    long somme = 0;
    for (int x : t) somme += x;
    return (int) (attendu - somme);
}
```
Alternative élégante sans risque de débordement : un XOR de tous les indices et de toutes les valeurs.

**19. Deuxième plus grand élément distinct**
```java
static OptionalInt deuxiemePlusGrand(int[] t) {
    return Arrays.stream(t).boxed().distinct().sorted(Comparator.reverseOrder())
        .skip(1).mapToInt(Integer::intValue).findFirst();
}
```
Version en O(n) attendue souvent ensuite : un seul parcours en maintenant le premier et le deuxième maximum.

**20. Compression de chaîne** (`"aaabccdddd"` → `"a3b1c2d4"`, on renvoie l'original si ce n'est pas plus court)
```java
static String compresser(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); ) {
        int j = i;
        while (j < s.length() && s.charAt(j) == s.charAt(i)) j++;
        sb.append(s.charAt(i)).append(j - i);
        i = j;
    }
    return sb.length() < s.length() ? sb.toString() : s;
}
```

**21. Exercices de Streams sur une liste d'employés**
```java
record Employe(String nom, String departement, double salaire, int age) {}

// Salaire moyen par département, trié par département
Map<String, Double> moyenne = emps.stream()
    .collect(groupingBy(Employe::departement, TreeMap::new, averagingDouble(Employe::salaire)));

// Nom de l'employé le mieux payé de chaque département
Map<String, String> mieuxPaye = emps.stream()
    .collect(groupingBy(Employe::departement, TreeMap::new,
        collectingAndThen(maxBy(comparingDouble(Employe::salaire)), o -> o.get().nom())));

// Séparer les plus de 40 ans des autres, noms joints par une virgule
Map<Boolean, String> parAge = emps.stream()
    .collect(partitioningBy(e -> e.age() > 40, mapping(Employe::nom, joining(","))));

// Trier par département puis salaire décroissant
List<String> tries = emps.stream()
    .sorted(comparing(Employe::departement).thenComparing(Employe::salaire, reverseOrder()))
    .map(Employe::nom).toList();

// Masse salariale totale, et deuxième salaire le plus élevé
double total = emps.stream().mapToDouble(Employe::salaire).sum();
Optional<String> deuxieme = emps.stream()
    .sorted(comparingDouble(Employe::salaire).reversed())
    .skip(1).findFirst().map(Employe::nom);
```

**22. Classiques de conception déjà traités ailleurs** : cache LRU (chapitre Collections), producteur-consommateur avec `BlockingQueue` (chapitre Concurrence), singleton thread-safe (chapitre Design Patterns : idiome holder ou enum), classe immuable (chapitre Fondamentaux : champs `final`, classe `final`, pas de setter, copies défensives), `equals`/`hashCode` corrects (chapitre Fondamentaux), compteur thread-safe (`AtomicInteger`, `LongAdder`), parallélisation d'appels avec `CompletableFuture.allOf` (chapitre Concurrence).

### En situation

- **"Écris une fonction qui..."** → reformuler, cas limites, solution simple, puis optimisée.
- **"Peux-tu faire mieux ?"** → presque toujours une HashMap, deux pointeurs ou un tri préalable.
- **"Et si les données ne tiennent pas en mémoire ?"** → traitement en flux, par lots, tri externe, ou délégation à la base.
- **"Et en concurrence ?"** → structures concurrentes, immutabilité, ou isolement par thread.

### Questions techniques

**Q : Complexité de Two Sum avec une HashMap ?** R : O(n) en temps, O(n) en mémoire.

**Q : Comment détecter un cycle dans une liste chaînée sans mémoire supplémentaire ?** R : Algorithme de Floyd : un pointeur lent et un rapide finissent par se rencontrer s'il y a un cycle.

**Q : Pourquoi le Fibonacci récursif naïf est-il lent ?** R : Il recalcule les mêmes sous-problèmes : complexité exponentielle.

**Q : Quelle structure pour vérifier des parenthèses ?** R : Une pile (ArrayDeque).

**Q : Comment trouver le premier élément non répété en conservant l'ordre ?** R : Compter dans une LinkedHashMap puis prendre la première entrée de compte 1.

**Q : Comment conserver uniquement les K plus grands éléments d'un flux ?** R : Un min-heap de taille K.

**Q : Quelle est l'idée de l'algorithme de Kadane ?** R : À chaque position, prolonger le sous-tableau courant ou en recommencer un, en gardant le meilleur.

**Q : Qu'est-ce qu'une fenêtre glissante ?** R : Deux indices qui délimitent une sous-séquence contiguë et avancent sans tout recalculer.

**Q : Pourquoi BigInteger pour une factorielle ?** R : 21! dépasse la capacité d'un long.

### Questions de situation

**Q : "Tu bloques sur un exercice en live coding."**
R : "Je le dirais, et je reviendrais au plus simple : écrire une solution naïve qui fonctionne, même en O(n²), plutôt que de rester sans rien. Je tracerais un petit exemple à la main pour voir le motif, et je me demanderais quelle structure rend la recherche en O(1) ou si trier d'abord simplifie le problème.
L'évaluateur préfère un candidat qui raisonne à voix haute et avance par étapes, quitte à accepter un indice, à un candidat silencieux. Accepter une suggestion et l'exploiter fait aussi partie de ce qui est évalué : c'est ce qui se passe en binôme au quotidien."
