---
subject: ci-cd
title: CI/CD & GitOps
category: cloud-devops
position: 120
status: published
---

# CI/CD & GitOps : le document de référence pour les entretiens techniques

> Pour préparer un entretien : intégration et déploiement continus, GitOps.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. CI/CD & GITOPS
═══════════════════════════════════════════

### Vue d'ensemble

L'intégration continue répond à un problème historique : quand des développeurs travaillent des semaines chacun de son côté, l'intégration finale est douloureuse et imprévisible ("integration hell"). La CI inverse la logique (**intégrer très souvent, en petits incréments, avec une validation automatique**) de sorte que chaque écart est détecté immédiatement, quand il est encore petit.

Le déploiement continu prolonge l'idée jusqu'à la production. L'intuition contre-intuitive est que **déployer plus souvent réduit le risque** : de petits changements fréquents sont plus faciles à valider, à comprendre et à annuler qu'une grosse livraison trimestrielle.

Le **GitOps** ajoute une dernière pièce : décrire l'état désiré de l'infrastructure dans Git et laisser un opérateur réconcilier le cluster. On retrouve exactement le modèle déclaratif de Kubernetes, appliqué au déploiement, avec un historique auditable et un rollback qui n'est qu'un `git revert`.

### Cours

**CI** : à chaque commit, build + tests + analyse automatiques. Prérequis : une suite de tests fiable et rapide.

**Continuous Delivery** : le code est toujours déployable, la mise en production reste un acte volontaire.
**Continuous Deployment** : tout ce qui passe la validation part automatiquement en production.

**Pipeline typique** : checkout → build → tests unitaires → analyse qualité/sécurité → build image → publication registry → déploiement dev → tests d'intégration → promotion vers les environnements supérieurs.

**Jenkins** : serveur d'automatisation, pipeline déclaré en `Jenkinsfile` versionné avec le code. Alternatives : GitLab CI, GitHub Actions, Tekton.

**GitOps** : Git est la source unique de vérité de l'infrastructure. Un opérateur (**ArgoCD**, Flux) surveille le dépôt et synchronise le cluster. Bénéfices : audit complet (chaque changement est un commit signé et revu), rollback par `git revert`, **détection de drift** (alerte si le cluster diverge de Git), et suppression des `kubectl apply` manuels.

Bonne pratique : **séparer le dépôt applicatif du dépôt de manifests**, pour que la CI applicative n'ait pas les droits sur l'infrastructure.

**Helm** : package manager Kubernetes. Un **chart** est un ensemble de templates paramétrés par un `values.yaml`, versionnable et réutilisable entre environnements. Alternative : Kustomize (surcharges sans templating).

**Stratégies de déploiement :**
- **Rolling** : remplacement progressif, par défaut.
- **Blue-Green** : deux environnements complets, bascule instantanée du trafic, rollback immédiat, mais double le coût d'infrastructure.
- **Canary** : une fraction du trafic sur la nouvelle version, observation, progression graduelle. Le meilleur compromis risque/coût.
- **Feature flags** : découpler le déploiement de l'activation. On livre du code désactivé, puis on l'active pour une population ciblée, indispensable en trunk-based.

**Argo Rollouts** : automatise canary et blue-green avec **analyse de métriques** : la progression n'a lieu que si les indicateurs restent sains.

**Sécurité du pipeline** : secrets dans un vault et jamais dans le code ni les logs, scan des images, principe du moindre privilège pour les comptes de service, signature des artefacts.

**Les plateformes de CI** :
- **Jenkins** : pipeline **déclaratif** (`pipeline { agent ...; stages { stage('Build') { steps { sh 'mvn -B verify' } } }; post { always { junit '...' } } }`, structuré et lisible) ou **scripté** (Groovy libre, plus puissant, plus difficile à maintenir) ; agents (nœuds d'exécution, souvent des pods Kubernetes éphémères), *shared libraries* pour mutualiser du code de pipeline entre dépôts, plugins nombreux (et à maintenir).
- **GitHub Actions** : *workflows* YAML dans `.github/workflows`, déclencheurs (`on: push`, `pull_request`, `workflow_dispatch`, `schedule`), *jobs* exécutés sur des *runners*, *steps* qui utilisent des *actions* réutilisables (`actions/checkout`, `actions/setup-java` avec cache Maven), matrices (plusieurs versions de JDK), secrets et environnements protégés, authentification OIDC vers le cloud sans secret longue durée.
- **GitLab CI** : `.gitlab-ci.yml`, *stages* et *jobs*, *runners*, artefacts et cache, environnements, *merge request pipelines*.
- **Tekton** (natif Kubernetes, tâches et pipelines en CRD), Azure DevOps, CircleCI.

**Artefacts vs cache** : un **artefact** est un produit du pipeline transmis à une étape suivante ou conservé (le jar, un rapport de tests) ; le **cache** accélère des exécutions futures (dépôt Maven), il peut être perdu sans conséquence.

**Principe "build once, deploy many"** : on construit **une seule fois** l'image, identifiée par une version immuable (ou le SHA du commit), et c'est **la même image** qui est promue de dev en recette puis en production ; seule la configuration change. Reconstruire pour chaque environnement casse la garantie que ce qui a été testé est ce qui part en production.

**Qualité et sécurité dans le pipeline** (*shift left*) : tests et couverture, **SonarQube** avec quality gate, **SAST** (analyse du code : Sonar, Semgrep, CodeQL), **SCA** (dépendances vulnérables : OWASP Dependency-Check, Dependabot, Renovate, Snyk), scan de l'image (Trivy, Grype), détection de secrets (gitleaks), **SBOM** (*Software Bill of Materials*, inventaire des composants au format CycloneDX ou SPDX), **signature** des images et attestations de provenance (Sigstore/cosign, cadre SLSA), DAST sur l'environnement de recette.

**ArgoCD, compléments** : une `Application` relie un dépôt (chemin, révision) à une destination (cluster, namespace) ; synchronisation manuelle ou automatique, `prune` (supprimer ce qui n'est plus dans Git), `selfHeal` (annuler une modification manuelle), *sync waves* et *hooks* pour ordonner (migrations avant déploiement), pattern **app of apps** ou `ApplicationSet` pour gérer de nombreux environnements et clusters. **Flux** est l'autre outil GitOps majeur.

**Promotion entre environnements en GitOps** : un répertoire ou une branche par environnement dans le dépôt de manifestes ; promouvoir = une PR qui met à jour le tag de l'image dans l'environnement suivant (automatisable avec Argo CD Image Updater ou par la CI), avec revue pour la production.

**Bases de données dans le pipeline** : migrations Flyway/Liquibase exécutées avant le déploiement de la nouvelle version (job dédié ou hook de synchronisation), toujours rétrocompatibles avec la version qui tourne encore.

### En situation

- **"Décris ton pipeline"** → du commit au pod.
- **"Rollback en urgence"** → revert, rollout undo, bascule canary.
- **"Le pipeline est trop lent"** → cache, parallélisation, découpage.
- **"Comment sécuriser le pipeline ?"** → secrets, scans, droits.

### Questions techniques

**Q : Quel problème résout la CI ?** R : L'integration hell, en intégrant souvent et en petits incréments, les écarts sont détectés tôt.

**Q : Delivery vs Deployment ?** R : Mise en production manuelle vs automatique.

**Q : Principe du GitOps ?** R : L'état désiré est dans Git ; un opérateur réconcilie le cluster en continu.

**Q : Avantages du GitOps ?** R : Audit, rollback par revert, détection de drift, plus de kubectl manuel.

**Q : C'est quoi un chart Helm ?** R : Un ensemble de templates Kubernetes paramétrés par values.yaml, versionnable.

**Q : Blue-green vs canary ?** R : Deux environnements avec bascule instantanée (coûteux) vs progression graduelle du trafic.

**Q : À quoi servent les feature flags ?** R : Découpler le déploiement de l'activation, livrer du code inactif, l'activer ensuite.

**Q : Pourquoi séparer dépôt applicatif et dépôt de manifests ?** R : Pour que la CI applicative n'ait pas les droits sur l'infrastructure.

**Q : Pipeline Jenkins déclaratif vs scripté ?** R : Structure imposée et lisible vs Groovy libre plus puissant mais moins maintenable.

**Q : À quoi servent les shared libraries Jenkins ?** R : Mutualiser du code de pipeline entre de nombreux dépôts.

**Q : Les éléments d'un workflow GitHub Actions ?** R : Déclencheurs, jobs, runners, steps, actions réutilisables, secrets, matrices.

**Q : Artefact vs cache ?** R : Produit du pipeline transmis ou conservé vs accélérateur d'exécutions futures, jetable.

**Q : C'est quoi "build once, deploy many" ?** R : Construire une seule image et promouvoir la même d'environnement en environnement, seule la configuration changeant.

**Q : C'est quoi un SBOM ?** R : L'inventaire des composants et dépendances d'un logiciel (CycloneDX, SPDX).

**Q : À quoi sert cosign ?** R : Signer et vérifier des images de conteneur et des attestations (Sigstore).

**Q : SAST vs SCA ?** R : Analyse du code maison vs analyse des dépendances tierces.

**Q : prune et selfHeal dans ArgoCD ?** R : Supprimer les ressources retirées de Git / annuler les modifications manuelles faites sur le cluster.

**Q : C'est quoi le pattern app of apps ?** R : Une Application ArgoCD qui déclare d'autres Applications, pour gérer de nombreux services et environnements.

**Q : Comment s'authentifier au cloud depuis une CI sans secret stocké ?** R : Avec la fédération OIDC : la CI obtient un jeton éphémère échangé contre des droits cloud.

### Questions de situation

**Q : "Décris ton pipeline de déploiement de bout en bout."**
R : "Du commit au pod en production.
Je pousse sur une branche, ce qui déclenche la CI (**Jenkins**, GitHub Actions ou GitLab CI) : compilation Maven, tests unitaires et d'intégration, analyse de qualité et de couverture. Si tout passe, construction d'une **image Docker** en multi-stage et publication dans le registry interne, taguée avec la version.
Le tag est ensuite mis à jour dans le **dépôt GitOps** où vivent les manifests et les values Helm. C'est ce commit qui constitue la véritable déclaration de déploiement.
**ArgoCD** détecte le changement et synchronise le cluster Kubernetes (ou OpenShift). Pour les déploiements sensibles, **Argo Rollouts** fait du canary : une fraction du trafic, observation des métriques, progression.
L'intérêt du modèle : personne ne fait de `kubectl apply` manuel. Chaque changement d'infrastructure est un commit, donc revu, auditable et réversible, un rollback est un `git revert`."

**Q : "Une version déployée provoque des erreurs en production."**
R : "La priorité est de **rétablir le service**, pas de comprendre le bug. On diagnostique après.
Rollback immédiat : en GitOps, un `git revert` du commit de déploiement, et ArgoCD resynchronise. Si le temps presse davantage, `kubectl rollout undo` agit directement, mais il faut ensuite réaligner Git, sinon ArgoCD détectera un drift et remettra la version fautive.
Avec un canary, c'est plus simple encore : on annule la progression, le trafic revient sur la version stable, et l'impact s'est limité au pourcentage exposé.
Le point d'attention reste la **base de données** : si la version fautive a appliqué une migration destructive, revenir sur le code ne suffit pas, d'où la règle des migrations toujours rétrocompatibles.
Une fois le service rétabli, analyse à froid : logs, traces, métriques, reproduction en test. Et une question de fond : pourquoi la CI et le canary ne l'ont pas détecté ? La réponse renforce les garde-fous."

**Q : "Conçois un pipeline pour un microservice Java, du commit à la production."**
R : "Sur chaque **pull request** : build Maven avec cache, tests unitaires et d'intégration (Testcontainers), analyse Sonar avec quality gate, scan des dépendances et détection de secrets. Rapide, sous dix minutes, et bloquant pour le merge.
Au **merge sur main** : construction d'**une** image en multi-stage, tag immuable (version ou SHA), scan de l'image, génération du SBOM, signature, publication dans le registry. Puis mise à jour du tag dans le dépôt GitOps de l'environnement de dev, ArgoCD déploie, tests de fumée et tests de contrat.
**Promotion** vers la recette puis la production par PR sur le dépôt GitOps, la même image, avec revue pour la production. En production, **canary** avec Argo Rollouts et analyse automatique des métriques d'erreur et de latence, rollback automatique si elles se dégradent.
Transversalement : secrets dans un vault, comptes de service à moindre privilège, et suivi des métriques DORA pour améliorer le pipeline lui-même."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **CI/CD** | Continuous Integration / Continuous Delivery ou Deployment |
| **DORA** | Métriques de performance de livraison (fréquence, délai, taux d'échec, temps de restauration) |
| **GitOps** | Git comme source de vérité de l'infrastructure, réconciliée par un opérateur (ArgoCD, Flux) |
