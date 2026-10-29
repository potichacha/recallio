# Document de test

Introduction ignorée.

═══════════════════════════════════════════
# 1. JAVA : FONDAMENTAUX
═══════════════════════════════════════════

### Vue d'ensemble

Texte de cours ignoré, même avec **Q : une fausse question ?** R : dans le cours.

### Cours

```bash
# ce commentaire bash n'est pas un titre de chapitre
echo ok
```

### Questions techniques

**Q : Qu'est-ce que la **JVM** ?** R : La machine virtuelle qui exécute le bytecode.

**Q : Quelle est la taille d'un `int` ?** R : 32 bits, signé.

Une ligne orpheline qui ne correspond à rien.

### Questions de situation

**Q : "Deux objets identiques sont différents dans une HashMap. Comment tu investigues ?"**
R : "Je vérifie `equals` et `hashCode`.
Ensuite, je vérifie l'immutabilité des champs."

**Q : "Comment tu optimises un projet Quarkus ?"** *(très fréquent)*
R : "Je mesure d'abord :
```java
Instant start = Instant.now();
```
puis j'optimise le point chaud."

═══════════════════════════════════════════
# 14. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?
═══════════════════════════════════════════

### Questions techniques

**Q : `System.out.println(0.1 + 0.2 == 0.3);`** R : `false` : imprécision des flottants.

**Q :**
```java
Integer a = 1000, b = 1000;
System.out.println(a == b);
```
R : `false` : `==` compare les références hors du cache.

**Q : Une question sans réponse ?**

### Questions de situation

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

Un index rapide.

| Terme | Signification |
|---|---|
| **ACID** | Atomicity, Consistency, Isolation, Durability : garanties des transactions |
| **JVM** | Java Virtual Machine |

═══════════════════════════════════════════
# RÉCAP FINAL
═══════════════════════════════════════════

**Q : Ignorée car hors chapitre ?** R : Oui.
