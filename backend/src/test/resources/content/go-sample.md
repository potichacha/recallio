---
subject: go
title: Go
category: languages
position: 20
status: published
---
# Go : document de test

Utilisé par les tests de révision multi-sujets.

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
