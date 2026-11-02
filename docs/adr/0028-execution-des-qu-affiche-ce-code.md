# 0028. Exécuter les « qu'affiche ce code ? » en CI

- Statut : accepté
- Date : 2026-11-02

## Contexte

La refonte du contenu (ADR 0025) demande que « le code soit vérifié » : la CI doit exécuter chaque « qu'affiche ce
code ? » et comparer sa sortie à la réponse. Deux obstacles :

- la réponse est de la prose (« `100` : `i - 1` est un int, autoboxé… »), qu'une machine ne sait pas comparer à une
  sortie ;
- la question est souvent un fragment : une instruction sans classe ni `main`, une expression suivie de « ? », plusieurs
  expressions à la suite, ou du code avec `...`.

## Options envisagées

**Où écrire le résultat attendu** :
- l'extraire de la réponse (le premier passage entre accents graves) : fragile, et muet sur les exceptions et les erreurs
  de compilation ;
- **un champ structuré, propre à la CI** (retenu), à côté de la réponse rédigée.

**Comment exécuter un fragment** :
- l'envelopper dans une classe et un `main`, le compiler avec `javax.tools`, puis le lancer : il faut des imports et un
  découpage entre déclarations et instructions ;
- un bac à sable (Judge0, Piston, conteneurs) : c'est la décision de l'étape 4, pour le code des joueurs. Notre contenu
  est relu par pull request et n'en a pas besoin ;
- **l'API JShell (`jdk.jshell`)** (retenue) : fournie avec le JDK, aucune dépendance. Elle exécute directement
  instructions, expressions et déclarations, ce qui correspond à nos fragments.

## Décision

Chaque carte `code-output` d'un dossier de sujet porte **soit** une vérification `check`, **soit** une raison
`unverifiable` (une sortie non déterministe : ordre des threads, `hashCode` d'identité...) :

```yaml
check:
  code: |-                     # facultatif : par défaut, le premier bloc java de la question
    System.out.println(Math.round(-2.5) + " " + Math.round(2.5));
  output: "-2 3"               # sortie standard exacte, aux fins de ligne près
  throws: NullPointerException # facultatif : l'exception qui arrête le code, après sa sortie
  # ou bien, seul : compiles: false
```

- `check` sert à la CI, n'est ni importé ni montré, et n'entre pas dans l'empreinte de la carte : l'ajouter ne crée pas
  de version.
- Le parseur vérifie sa **forme** : réservé au format `code-output`, `check` ou `unverifiable` mais pas les deux, et
  `compiles: false` seul ou une sortie ou une exception. Une erreur de forme est une anomalie du rapport d'import.
- La CI (`CodeOutputTest`, job `content`) vérifie sa **présence**. Elle exécute le code et compare la sortie, l'exception
  et le fait qu'il compile. L'import n'est pas bloqué par une vérification manquante.
- L'exécuteur (`SnippetRunner`, côté tests) lance une seule JVM JShell, distincte de celle des tests, pour toutes les
  cartes. Il charge les imports par défaut de l'outil `jshell` (`java.util.*`, `java.util.stream.*`...), retire les
  déclarations d'un extrait avant le suivant, et interrompt un extrait qui dépasse dix secondes.
- Seul Java est exécuté. Go reste un document Markdown : ses questions pièges seront exécutées quand il sera migré.

## Conséquences

- Une réponse fausse, ou qui ne vaut plus pour le JDK du projet, se voit avant le merge. Dès sa mise en place, le
  contrôle a trouvé une carte qui ne compilait pas (`Optional.ofNullable(null).map(String::length)` : `T` est inféré en
  `Object`).
- Écrire une carte demande d'écrire aussi le code exécutable et la sortie exacte : quelques lignes de plus, et un auteur
  qui doit avoir réellement exécuté son code.
- JShell n'est pas tout à fait Java : au premier niveau, une variable est un champ et `final` ou `static` est ignoré. Une
  carte qui dépend des variables locales (une constante `final` calculée à la compilation) place son code dans une
  méthode (`void run() { ... } run();`). Le guide de rédaction le rappelle.
- Le résultat attendu est maintenant structuré. Il pourra servir plus tard à corriger une sortie saisie, ou à générer un
  QCM pour les parties live (CLAUDE.md, section 5.3).
