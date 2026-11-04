# Rétrospective : production du contenu Go (phase 3 bis, après Java)

- Période : 2026-11-04
- Livré : une seule PR de contenu (branche `content/go-complete`), après l'infrastructure du bac à sable Go
  (ADR 0033, PR #63)

## Ce qui a été produit

Go a suivi le même chemin que Java : un référentiel, un dossier de notions, 4 cartes par format et des exercices jugés
par leurs tests. Toutes les notions sont déclarées complètes (`complete: true`) :

| Priorité | Notions | Complètes |
|---|---|---|
| 1 | 98 | 98 |
| 2 | 78 | 78 |
| 3 | 40 | 40 |
| 4 | 7 | 7 |
| **Total** | **223** | **223** |

- 17 domaines, du langage de base aux pratiques (outillage, observabilité, sécurité, résilience) ;
- **4 893 cartes**, dont les 745 du document d'entretien d'origine, migrées sans perte de progression
  (`content/source/go-entretien-reference.md` est supprimé) ;
- une leçon par notion (223) ;
- 451 « qu'affiche ce code ? », exécutés par la chaîne d'outils Go en CI (`GoSnippetRunner`) ;
- 21 exercices de code, joués dans le bac à sable Go (le code de départ échoue, la solution passe).

## Ce qui a bien marché

- **Le même outillage que pour Java**, adapté à Go : fusion des brouillons, génération du programme complet de chaque
  « qu'affiche ce code ? » à partir du fragment montré, vérification locale rapide en parallèle avant la CI.
- **L'exécution des cartes de code** a rattrapé des erreurs de sortie (espaces de `fmt.Print`, ordre des imports
  déduits, déclarations de package) avant le merge.
- **Les exercices de concurrence sans durée** : les tests attendent que les goroutines se rejoignent (une barrière)
  avec un délai de garde court ; la solution parallèle passe toujours, le code de départ séquentiel échoue vite.

## Ce qui a coincé

- **Le cache du bac à sable** : un exercice qui importait `testing/iotest` passait en local mais échouait dans le bac à
  sable (« package testing/iotest is not in std »). Le cache de compilation est en lecture seule et ne contient que les
  paquets de `sandbox/go/warm/warm.go` ; le commentaire de ce fichier annonce pourtant qu'un autre paquet « sera
  compilé à chaque exécution, donc plus lentement ». L'exercice utilise maintenant des lecteurs écrits dans le test, et
  la règle est dans `content/README.md`. **À trancher** : corriger le commentaire, ou rendre le cache inscriptible dans
  le tmpfs pour que les paquets non précompilés fonctionnent (ADR 0033).
- **Les sorties non déterministes** : la concurrence et les maps obligent à choisir des exemples dont le résultat ne
  dépend pas de l'ordonnancement (sommes, résultats rangés par indice, `fmt` qui trie les maps).
- **Les messages d'erreur du runtime** : `panics` accepte aussi les erreurs fatales (« all goroutines are asleep »),
  ce qui permet des cartes sur les blocages.

## Faits à relire en priorité

Ces cartes énoncent des faits que rien n'exécute, sur des versions récentes ou mouvantes :

- **Go 1.27** : les méthodes génériques (`generics-limites`, `nouveautes-versions`), et `encoding/json` reposant sur
  l'implémentation v2 (`encoding-json`) ; la chaîne d'outils locale est en 1.26, donc rien n'a été exécuté en 1.27 ;
- **Go 1.25 et 1.26** : GOMAXPROCS adapté aux limites CPU des conteneurs et sa réévaluation (`gomaxprocs`), le GC
  Green Tea (`garbage-collector`), le flight recorder (`trace-execution`), `go fix` et ses modernisations (vérifiées
  contre `go tool fix help` en 1.26.2) ;
- **les valeurs par défaut citées** : `MaxIdleConnsPerHost` (2), `MaxIdleConns` de `sql.DB` (2), la taille initiale
  des piles des goroutines, la période de préemption (10 ms) ;
- **l'écosystème** : clients Kafka, golangci-lint (format de configuration v2), Kubebuilder, Connect, ko ;
- **la cryptographie** : FIPS 140-3, ML-KEM, `rand.Text`, `http.CrossOriginProtection`.

## Ce qu'on change

1. **La gamification v2** (phase 3 ter) est la suite : les deux langages complets donnent assez de matière pour la
   maîtrise par notion et les quêtes qui font varier les formats.
2. **Le bac à sable Go** : décider du cache (ci-dessus) avant le prochain sujet qui aura des exercices.
3. **Les sujets sortis du document Java** (Spring Boot, Docker, SQL...) restent des documents d'entretien à enrichir ;
   les migrer vers des dossiers de notions demandera le même effort que Go, sujet par sujet.
