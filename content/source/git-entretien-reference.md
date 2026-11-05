---
subject: git
title: Git
category: cloud-devops
position: 150
status: draft
---

# Git : le document de référence pour les entretiens techniques

> Pour préparer un entretien : le modèle de Git, les branches, la fusion et l'historique.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. GIT
═══════════════════════════════════════════

### Vue d'ensemble

Git est un système de contrôle de version **distribué** : chaque développeur possède l'historique complet en local, ce qui permet de travailler et de committer sans réseau. Techniquement, Git n'enregistre pas des différences mais des **instantanés** du projet ; chaque commit pointe vers son ou ses parents, formant un graphe orienté acyclique. Une branche n'est qu'un **pointeur mobile** sur un commit, c'est pourquoi créer une branche est instantané.

Comprendre ce modèle éclaire tout le reste : `merge` crée un commit ayant deux parents, `rebase` recrée de nouveaux commits ailleurs dans le graphe (d'où le danger sur une branche partagée), et `reset` déplace simplement un pointeur (d'où la possibilité de récupérer via `reflog`).

### Cours

**Les trois zones** : working directory (fichiers modifiés) → staging area / index (`git add`) → repository (`git commit`).

**fetch vs pull** : `fetch` récupère sans intégrer ; `pull` = `fetch` + `merge` (ou `rebase` avec `--rebase`).

**merge vs rebase** : `merge` crée un commit de fusion et conserve l'historique réel avec ses embranchements. `rebase` rejoue tes commits par-dessus la cible, produisant un historique linéaire, mais il crée de **nouveaux** commits. D'où la règle d'or : **jamais rebaser une branche déjà partagée**, sous peine de casser le dépôt des autres.

**reset** : `--soft` (annule le commit, garde les changements indexés), `--mixed` (garde les changements non indexés), `--hard` (perd tout). **revert** crée un commit qui annule un commit antérieur, c'est la seule option sûre sur une branche partagée.

**Outils** : `cherry-pick` (appliquer un commit précis ailleurs), `stash` (mettre de côté), `rebase -i` (réorganiser, squash, reword), `reflog` (historique de HEAD, permet de récupérer après un `reset --hard`), `bisect` (recherche dichotomique du commit fautif), `blame` (qui a modifié chaque ligne), `tag` (marquer une version).

**Stratégies de branching** :
- **Git Flow** : main, develop, feature, release, hotfix. Adapté aux releases planifiées et versionnées ; lourd pour du déploiement continu.
- **GitHub Flow** : main + branches de fonctionnalité, PR, merge. Simple, adapté au déploiement continu.
- **Trunk-Based** : tout le monde intègre sur le tronc très fréquemment, les fonctionnalités incomplètes sont masquées par des **feature flags**. Exige une bonne CI, mais c'est le modèle des équipes à haute fréquence de déploiement.

**Bonnes pratiques** : commits **atomiques** (un commit = un changement cohérent), messages explicites (idéalement conventionnels : `feat:`, `fix:`), branches courtes, revue systématique.

**Conflits** : ils surviennent quand deux branches modifient les mêmes lignes. On les résout en comprenant les deux intentions, pas en choisissant au hasard.

**Objets Git** : *blob* (contenu d'un fichier), *tree* (répertoire), *commit* (un tree + parents + auteur + message), *tag* annoté. Chaque objet est identifié par un hash de son contenu (SHA-1, migration vers SHA-256 en cours). **HEAD** pointe vers la branche courante ; en **HEAD détaché**, il pointe directement vers un commit, et les commits faits dans cet état peuvent être perdus si on ne crée pas de branche.

**Types de merge** : *fast-forward* (la branche cible n'a pas avancé, Git déplace simplement le pointeur, pas de commit de fusion), `--no-ff` (force un commit de fusion pour garder la trace de la branche), *squash merge* (tous les commits de la branche deviennent un seul, historique propre mais détail perdu), *three-way merge* (commit de fusion à partir de l'ancêtre commun).

**Commandes modernes et utiles** : `git switch` / `git restore` (Git 2.23, séparent les deux rôles de `checkout`), `git commit --amend` (modifier le dernier commit, à éviter s'il est poussé), `git rebase -i HEAD~5` (squash, fixup, reword, réordonner), `git commit --fixup <sha>` + `git rebase -i --autosquash`, `git rebase --onto`, `git stash push -m "..."` / `stash pop` / `stash list`, `git worktree add` (plusieurs branches extraites en même temps), `git log --oneline --graph --all`, `git diff --staged`, `git blame -w`, `git reflog`, `git cherry-pick -x`, `git clean -fd` (fichiers non suivis), `git push --force-with-lease` (force push qui échoue si quelqu'un a poussé entre-temps, à préférer à `--force`), `git tag -a v1.2.0 -m "..."`.

**.gitignore** : n'affecte que les fichiers non encore suivis ; pour un fichier déjà commité, `git rm --cached fichier` puis l'ajouter au .gitignore.

**Hooks** : scripts exécutés à certains moments (`pre-commit` pour formater ou détecter des secrets, `commit-msg` pour vérifier le format du message, `pre-push`). Côté client, ils sont contournables (`--no-verify`) : les règles impératives se vérifient aussi en CI.

**Signature des commits** (GPG ou SSH) : prouve l'auteur ; souvent exigée sur les branches protégées.

**Protection de branche** : interdire le push direct sur main, exiger une PR, des relecteurs, une CI verte, un historique linéaire.

**Submodules vs monorepo vs polyrepo** : les submodules (un dépôt dans un dépôt, figé à un commit) sont réputés pénibles ; un **monorepo** facilite les changements transverses et le partage de code mais demande des outils de build incrémental ; le **polyrepo** isole les équipes au prix de la coordination des versions.

**Fichiers volumineux** : Git LFS.

### En situation

- **"Décris ton workflow"** → feature → PR → review → merge.
- **"Un secret a été poussé"** → révoquer d'abord, nettoyer ensuite.
- **"Trouver le commit fautif"** → bisect.
- **"Annuler un commit déjà poussé"** → revert, jamais reset.

### Questions techniques

**Q : Git stocke des diffs ou des snapshots ?** R : Des instantanés ; les diffs sont calculés à l'affichage.

**Q : C'est quoi une branche techniquement ?** R : Un pointeur mobile sur un commit, d'où sa création instantanée.

**Q : merge vs rebase ?** R : Commit de fusion préservant l'historique vs réécriture linéaire créant de nouveaux commits.

**Q : Pourquoi ne pas rebaser une branche partagée ?** R : Elle réécrit l'historique et casse les copies locales des autres.

**Q : reset --soft / --mixed / --hard ?** R : Garde indexé / garde non indexé / perd les changements.

**Q : reset vs revert ?** R : reset réécrit l'historique ; revert ajoute un commit d'annulation, sûr sur du partagé.

**Q : À quoi sert reflog ?** R : Récupérer des commits "perdus" après un reset ou un rebase raté.

**Q : Trunk-based, comment gérer une fonctionnalité incomplète ?** R : Avec des feature flags, qui découplent le déploiement de l'activation.

**Q : C'est quoi HEAD ?** R : Le pointeur vers la branche (ou le commit) courant.

**Q : C'est quoi un HEAD détaché ?** R : HEAD pointe directement sur un commit et non sur une branche ; les nouveaux commits risquent d'être perdus.

**Q : Fast-forward vs merge commit ?** R : Simple déplacement du pointeur quand la cible n'a pas divergé vs commit à deux parents.

**Q : Squash merge, avantages et inconvénients ?** R : Historique propre, un commit par fonctionnalité ; perte du détail des commits intermédiaires.

**Q : --force vs --force-with-lease ?** R : Écrase sans condition vs refuse si la branche distante a évolué depuis votre dernière récupération.

**Q : Comment arrêter de suivre un fichier déjà commité ?** R : git rm --cached puis l'ajouter au .gitignore.

**Q : switch/restore vs checkout ?** R : Les deux nouvelles commandes séparent changer de branche et restaurer des fichiers, que checkout mélangeait.

**Q : À quoi sert git worktree ?** R : Avoir plusieurs branches extraites simultanément dans des répertoires différents.

**Q : Pourquoi les hooks ne suffisent-ils pas ?** R : Ils sont locaux et contournables ; les règles doivent aussi être vérifiées en CI.

**Q : git stash pop vs apply ?** R : pop applique puis supprime l'entrée ; apply la conserve.

**Q : Monorepo, avantages ?** R : Changements atomiques transverses, partage de code, outillage unifié.

### Questions de situation

**Q : "Un commit parmi 200 a introduit un bug, on ne sait pas lequel."**
R : "**`git bisect`**, qui fait une recherche dichotomique. Je marque un commit bon et un mauvais, Git me place au milieu, je teste, je réponds, et il itère. Sur 200 commits ça converge en 8 étapes environ.
Si le bug est détectable par un script, `git bisect run` automatise entièrement la recherche.
Ça illustre aussi pourquoi les **commits atomiques** comptent : si chaque commit fait une seule chose, bisect pointe directement le changement fautif. Avec des commits fourre-tout de 2 000 lignes, on sait juste dans quel paquet chercher."

**Q : "Un mot de passe a été poussé il y a trois commits."**
R : "La première action, et c'est le réflexe essentiel : **révoquer le secret immédiatement**. Dès qu'il a été poussé, il doit être considéré comme compromis, il est dans l'historique local de tous ceux qui ont pull, potentiellement dans des caches de CI, des sauvegardes, des logs. Nettoyer le dépôt ne le décompromet pas.
Ensuite seulement, nettoyer l'historique avec `git filter-repo` ou BFG, ce qui implique un force-push et une coordination avec toute l'équipe puisqu'on réécrit l'historique.
En prévention : secrets dans un vault, `.gitignore` couvrant les fichiers de configuration locale, et un scan automatique dans la CI ou un hook de pré-commit qui détecte les patterns de secrets avant qu'ils ne partent."

**Q : "Tu as commité sur main au lieu d'une branche, sans avoir poussé."**
R : "Je crée une branche à cet endroit pour garder le travail (`git branch ma-feature`), puis je ramène main sur la version distante (`git reset --hard origin/main`), et je continue sur `ma-feature`. Rien n'est perdu, et si j'avais un doute, `git reflog` permettrait de retrouver n'importe quel état précédent."

**Q : "Ta branche a 30 commits 'wip' et 'fix typo' avant la revue."**
R : "Un `git rebase -i` sur la base de la branche pour regrouper en quelques commits cohérents (squash, fixup) avec des messages explicites, puis un `push --force-with-lease` puisque la branche n'est utilisée que par moi. Si l'équipe fait du squash merge, ce nettoyage est moins important, mais il facilite toujours la relecture commit par commit."
