---
subject: go
title: Go
category: languages
position: 20
status: published
---
# Go : document de test

Utilisé par les tests de révision multi-sujets. Seuls ses trois QCM se révisent : ses questions techniques n'ont pas de
choix (ADR 0063). La bonne réponse de chaque QCM est la deuxième.

═══════════════════════════════════════════════════════════════
# 1. GO : FONDAMENTAUX
═══════════════════════════════════════════════════════════════

### Questions techniques

**Q : Qu'est-ce qu'une goroutine ?** R : Une fonction exécutée de façon concurrente, ordonnancée par le runtime Go.

**Q : Que fait `defer` ?** R : Il diffère l'appel d'une fonction jusqu'au retour de la fonction englobante.

### QCM

**Q : Que produit la lecture d'un channel fermé et vide ?**
- [ ] Un blocage définitif
- [x] La valeur zéro du type, immédiatement
- [ ] Une panique
R : Une réception sur un channel fermé ne bloque jamais : elle renvoie la valeur zéro, et `ok` vaut `false` dans la forme `v, ok := <-ch`.

**Q : Quel mot-clé lance une goroutine ?**
- [ ] `async`
- [x] `go`
- [ ] `spawn`
R : `go f()` exécute `f` dans une nouvelle goroutine.

═══════════════════════════════════════════════════════════════
# 2. GO : MAPS
═══════════════════════════════════════════════════════════════

### QCM

**Q : Que vaut `len(m)` pour une map `m` à `nil` ?**
- [ ] Une panique
- [x] 0
R : Une map `nil` se lit comme une map vide : `len` vaut 0, seule l'écriture panique.
