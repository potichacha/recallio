---
subject: kubernetes
title: Kubernetes & OpenShift
category: cloud-devops
position: 110
status: published
---

# Kubernetes & OpenShift : le document de référence pour les entretiens techniques

> Pour préparer un entretien : Kubernetes et OpenShift.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. KUBERNETES & OPENSHIFT
═══════════════════════════════════════════

### Vue d'ensemble

Docker résout l'empaquetage d'une application ; Kubernetes résout l'**exploitation de centaines de conteneurs** sur un parc de machines. Sans orchestrateur, il faudrait décider à la main où placer chaque conteneur, le relancer quand il meurt, répartir le trafic, gérer les mises à jour.

Son principe fondateur est la **réconciliation déclarative** : on décrit l'**état désiré** ("je veux 3 instances de cette image"), et des contrôleurs comparent en boucle l'état réel à l'état désiré et agissent pour combler l'écart. On ne donne jamais d'ordre impératif, c'est ce qui rend le système auto-réparateur, et c'est aussi le fondement du GitOps.

Le corollaire pratique : pour que Kubernetes fasse bien son travail, il a besoin d'informations que **l'application doit lui fournir** : des sondes de santé pour savoir si elle va bien, des déclarations de ressources pour la placer correctement, et un arrêt propre pour ne pas couper de requêtes.

### Cours

**Objets principaux :**
- **Pod** : plus petite unité déployable ; un ou plusieurs conteneurs partageant réseau et volumes. Éphémère par nature.
- **ReplicaSet** : maintient N copies d'un pod.
- **Deployment** : déclare l'état désiré et pilote les ReplicaSets lors des mises à jour.
- **Service** : point d'accès réseau stable devant des pods dont les IP changent. `ClusterIP` (interne), `NodePort`, `LoadBalancer`.
- **Ingress** : routage HTTP externe par host/chemin (l'équivalent OpenShift s'appelle **Route**). Son successeur officiel est la **Gateway API** (voir plus bas).
- **ConfigMap** / **Secret** : configuration non sensible / sensible (encodée base64, **pas chiffrée** par défaut, il faut activer le chiffrement au repos ou utiliser un vault externe).
- **Namespace** : isolation logique (dev, staging, prod).
- **StatefulSet** : pour les applications à état, identité stable, stockage persistant attaché, ordre de démarrage garanti.
- **DaemonSet** : un pod par nœud (agents de logs, de monitoring).
- **Job / CronJob** : traitement ponctuel ou planifié.

**Sondes (probes)** : le contrat entre l'application et l'orchestrateur :
- **liveness** : "es-tu vivant ?", en échec, le pod est **redémarré**.
- **readiness** : "es-tu prêt à recevoir du trafic ?", en échec, le pod est **retiré du Service** sans redémarrage.
- **startup** : protège les applications lentes à démarrer, en suspendant les deux autres le temps du démarrage.

Erreur classique : une liveness avec un délai initial trop court tue une application qui n'a pas fini de démarrer, en boucle.

**Ressources** : **requests** = ce qui est réservé et sert au placement ; **limits** = plafond. Dépasser la limite mémoire tue le conteneur (**OOMKilled**) ; dépasser la limite CPU ne tue pas mais **throttle** : le service ralentit silencieusement, ce qui est plus difficile à diagnostiquer.

**HPA** (Horizontal Pod Autoscaler) : ajuste le nombre de replicas selon une métrique (CPU, mémoire, ou métrique personnalisée). Prérequis : l'application doit être **stateless**, sinon les requêtes d'un même utilisateur tombant sur des pods différents cassent.

**Mise à jour** : le rolling update remplace progressivement les pods (`maxSurge`, `maxUnavailable`). `kubectl rollout undo` revient à la version précédente.

**Arrêt propre** : à la suppression d'un pod, Kubernetes envoie un SIGTERM puis attend un délai de grâce. L'application doit cesser d'accepter de nouvelles requêtes et terminer celles en cours. Un `preStop` hook avec une courte pause laisse le temps au retrait des tables de routage.

**Réseau** : chaque pod a sa propre IP ; les Services fournissent un nom DNS interne stable. Les **NetworkPolicies** restreignent qui peut parler à qui.

**Sécurité** : ServiceAccount et RBAC (qui a le droit de faire quoi sur l'API), SecurityContext (non-root, système de fichiers en lecture seule).

**OpenShift** = Kubernetes + Routes, ImageStreams, BuildConfig (Source-to-Image), sécurité renforcée par défaut (SCC, interdiction du root), console web intégrée, opérateurs préinstallés.

**Architecture d'un cluster** :
- **Plan de contrôle** : `kube-apiserver` (point d'entrée unique, toute action passe par lui), **etcd** (base clé-valeur qui stocke tout l'état du cluster), `kube-scheduler` (choisit le nœud de chaque pod selon ressources, affinités, contraintes), `kube-controller-manager` (les boucles de réconciliation : Deployments, ReplicaSets, nœuds...), `cloud-controller-manager`.
- **Nœuds** : `kubelet` (agent qui fait tourner les pods et exécute les probes), le runtime de conteneurs (containerd, CRI-O), `kube-proxy` (règles réseau des Services, en iptables, IPVS ou eBPF selon le plugin réseau).
- **Plugin réseau CNI** (Calico, Cilium, Flannel) qui donne une IP à chaque pod et applique les NetworkPolicies.

**DNS interne** : un Service `api` dans le namespace `boutique` est joignable en `api.boutique.svc.cluster.local` (ou simplement `api` depuis le même namespace). Un Service **headless** (`clusterIP: None`) renvoie directement les IP des pods (utilisé par les StatefulSets).

**Exposition vers l'extérieur** : `Ingress` (règles HTTP, nécessite un contrôleur), et désormais **Gateway API** (`GatewayClass`, `Gateway`, `HTTPRoute`), son successeur officiel : modèle par rôles (infrastructure vs équipes applicatives), routage plus expressif (poids, en-têtes, gRPC, TCP), portable entre implémentations. Le contrôleur communautaire **Ingress NGINX a été retiré en mars 2026** (plus de correctifs de sécurité), ce qui pousse les équipes à migrer vers Gateway API ou un autre contrôleur.

**Stockage** : `PersistentVolume` (une ressource de stockage), `PersistentVolumeClaim` (une demande de stockage par une application), `StorageClass` (provisionnement dynamique selon un type : SSD, réseau). Modes d'accès : ReadWriteOnce, ReadOnlyMany, ReadWriteMany.

**Classes de QoS** des pods, qui déterminent l'ordre d'éviction en cas de pression sur le nœud : **Guaranteed** (requests = limits pour tous les conteneurs, évincé en dernier), **Burstable** (requests < limits), **BestEffort** (aucune ressource déclarée, évincé en premier).

**Placement** : `nodeSelector`, **affinités** et anti-affinités (de nœud ou de pod : par exemple répartir les replicas sur des nœuds et des zones différents), **taints et tolerations** (un nœud "repousse" les pods qui ne tolèrent pas sa marque : nœuds GPU, nœuds dédiés), `topologySpreadConstraints`.

**Disponibilité** : `PodDisruptionBudget` (nombre minimal de pods disponibles pendant une maintenance volontaire, comme la vidange d'un nœud), plusieurs replicas répartis, readiness correcte, anti-affinité.

**Autoscaling** : **HPA** (nombre de pods), **VPA** (ajuste requests/limits d'un pod), **Cluster Autoscaler** ou Karpenter (ajoute des nœuds), **KEDA** (scale sur des métriques d'événements, comme le lag Kafka ou la longueur d'une file, y compris jusqu'à zéro). Redimensionnement en place des ressources d'un pod (*in-place resize*), arrivé récemment dans les versions stables.

**Conteneurs auxiliaires** : *init containers* (s'exécutent jusqu'au bout avant les conteneurs principaux : attendre une dépendance, préparer des fichiers), *sidecars* (tournent à côté : proxy de service mesh, agent de logs ; Kubernetes les supporte nativement comme init containers avec `restartPolicy: Always`).

**Extensibilité** : **CRD** (Custom Resource Definitions, de nouveaux types d'objets) et **opérateurs** (contrôleurs qui encodent le savoir-faire d'exploitation d'un logiciel : bases de données, Kafka avec Strimzi, certificats avec cert-manager).

**Sécurité, compléments** : *Pod Security Standards* (`privileged`, `baseline`, `restricted`) appliqués par l'admission *Pod Security* par namespace (les PodSecurityPolicies ont été supprimées en 1.25) ; `securityContext` (`runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false`, capabilities supprimées) ; RBAC (`Role`/`ClusterRole` + `RoleBinding`) ; secrets externalisés (External Secrets Operator vers Vault ou un gestionnaire cloud, Sealed Secrets pour les stocker chiffrés dans Git) ; politique d'admission (Kyverno, OPA Gatekeeper) ; signature d'images.

**Statuts d'erreur fréquents** : `Pending` (aucun nœud n'a les ressources, ou PVC non lié, ou taint non toléré : `kubectl describe pod` le dit), `ImagePullBackOff` / `ErrImagePull` (image ou tag inexistant, registry privé sans `imagePullSecrets`), `CrashLoopBackOff` (le conteneur démarre puis meurt), `OOMKilled`, `Evicted` (pression mémoire ou disque sur le nœud), `CreateContainerConfigError` (ConfigMap ou Secret référencé absent).

**kubectl, les commandes à connaître** : `get pods -o wide`, `describe pod`, `logs -f <pod> [-c conteneur] [--previous]`, `exec -it <pod> -- sh`, `port-forward svc/api 8080:80`, `apply -f`, `delete`, `rollout status|history|undo deployment/api`, `scale --replicas=3`, `top pods` (metrics-server), `get events --sort-by=.lastTimestamp`, `explain`, `-n namespace`, `config use-context`, `debug` (conteneur éphémère de débogage dans un pod distroless).

**Service mesh** : Istio, Linkerd. Proxies (sidecar Envoy, ou mode *ambient* sans sidecar d'Istio) qui apportent mTLS automatique, retries, timeouts, répartition fine du trafic (canary), observabilité, sans code applicatif. Coût : complexité et ressources.

**Helm vs Kustomize, détail** : Helm est un gestionnaire de paquets (charts versionnés, templates Go, `values.yaml`, gestion des releases et rollback `helm rollback`, dépendances) ; Kustomize applique des **surcharges** (overlays par environnement) sur des manifestes YAML purs, sans templating, intégré à `kubectl apply -k`. On les combine aussi.

**Configuration** : une ConfigMap montée en **volume** est mise à jour dans le pod (avec un délai), mais une ConfigMap injectée en **variables d'environnement** ne change qu'au redémarrage du pod. Pour forcer un redémarrage quand la configuration change : une annotation de hash dans le template du Deployment (pratique courante avec Helm).

### En situation

- **"Un pod redémarre en boucle"** → logs `--previous`, événements, OOMKilled, probes.
- **"Comment scaler ?"** → HPA, stateless, limite de la base.
- **"Déployer sans coupure"** → rolling update + readiness + arrêt propre.
- **"Dimensionner requests et limits"** → mesurer, marge mémoire, méfiance sur le throttling CPU.

### Questions techniques

**Q : Quel est le principe de fonctionnement de Kubernetes ?** R : La réconciliation déclarative : des contrôleurs comparent l'état réel à l'état désiré et agissent en boucle.

**Q : Pod vs Deployment ?** R : Le pod est l'unité d'exécution éphémère ; le Deployment déclare et maintient l'état désiré via des ReplicaSets.

**Q : À quoi sert un Service ?** R : Fournir un point d'accès réseau stable devant des pods dont les IP changent.

**Q : liveness vs readiness vs startup ?** R : Vivant (redémarrage) / prêt (retrait du Service) / protège le démarrage lent.

**Q : requests vs limits ?** R : Réservé pour le placement vs plafond. Dépassement mémoire = kill ; dépassement CPU = throttling.

**Q : Un Secret est-il chiffré ?** R : Non, seulement encodé en base64 par défaut.

**Q : StatefulSet vs Deployment ?** R : Identité stable et stockage persistant par pod, pour les applications à état.

**Q : Prérequis d'un HPA ?** R : Une application stateless, et des métriques disponibles.

**Q : Que se passe-t-il à la suppression d'un pod ?** R : SIGTERM, puis délai de grâce ; l'application doit terminer proprement ses requêtes.

**Q : OpenShift vs Kubernetes ?** R : Routes, ImageStreams, BuildConfig, sécurité renforcée (non-root), console intégrée.

**Q : Les composants du plan de contrôle ?** R : kube-apiserver, etcd, kube-scheduler, kube-controller-manager (et cloud-controller-manager).

**Q : Rôle d'etcd ?** R : Stocker tout l'état du cluster (base clé-valeur distribuée et cohérente).

**Q : Rôle du kubelet ?** R : Faire tourner les pods sur le nœud et exécuter les probes.

**Q : Quel est le nom DNS d'un Service ?** R : nom.namespace.svc.cluster.local.

**Q : C'est quoi un Service headless ?** R : Un Service sans IP virtuelle dont le DNS renvoie directement les IP des pods.

**Q : Ingress vs Gateway API ?** R : Règles HTTP simples dépendantes d'annotations du contrôleur vs API plus riche, portable et organisée par rôles, successeur officiel.

**Q : Pourquoi migrer depuis Ingress NGINX ?** R : Le contrôleur communautaire a été retiré en mars 2026 et ne reçoit plus de correctifs de sécurité.

**Q : PV vs PVC vs StorageClass ?** R : Ressource de stockage vs demande de stockage vs type de stockage pour le provisionnement dynamique.

**Q : Les classes de QoS ?** R : Guaranteed, Burstable, BestEffort ; elles déterminent l'ordre d'éviction.

**Q : Taint vs affinité ?** R : Le nœud repousse les pods non tolérants vs le pod exprime une préférence ou une exigence de placement.

**Q : À quoi sert un PodDisruptionBudget ?** R : Garantir un nombre minimal de pods disponibles pendant les interruptions volontaires.

**Q : HPA vs VPA vs KEDA ?** R : Nombre de pods selon une métrique / ressources d'un pod / scaling sur des événements externes, jusqu'à zéro.

**Q : Init container vs sidecar ?** R : S'exécute jusqu'au bout avant l'application vs tourne en parallèle pendant toute la vie du pod.

**Q : C'est quoi un opérateur ?** R : Un contrôleur personnalisé qui automatise l'exploitation d'un logiciel via des ressources CRD.

**Q : Qu'a remplacé les PodSecurityPolicies ?** R : Les Pod Security Standards appliqués par l'admission Pod Security (PSP supprimées en 1.25).

**Q : Causes d'un pod Pending ?** R : Ressources insuffisantes, PVC non lié, taint non toléré, contraintes d'affinité impossibles.

**Q : Causes d'un ImagePullBackOff ?** R : Image ou tag inexistant, registry inaccessible, identifiants (imagePullSecrets) manquants.

**Q : Comment déboguer un pod distroless sans shell ?** R : kubectl debug avec un conteneur éphémère.

**Q : Comment accéder à un service du cluster depuis son poste ?** R : kubectl port-forward.

**Q : Helm vs Kustomize ?** R : Paquets templatisés versionnés avec gestion de releases vs surcharges de YAML purs par environnement.

**Q : Une ConfigMap modifiée est-elle prise en compte sans redémarrage ?** R : Montée en volume oui (avec délai) ; en variables d'environnement non.

**Q : Qu'apporte un service mesh ?** R : mTLS, retries, timeouts, routage fin et observabilité, sans modifier le code.

### Questions de situation

**Q : "Un pod est en CrashLoopBackOff."**
R : "Démarche systématique.
Les **logs** d'abord : `kubectl logs <pod> --previous`. Le `--previous` est essentiel, il donne les logs de l'instance qui vient de crasher, pas de celle qui redémarre.
Puis `kubectl describe pod` pour les **événements** et le motif de terminaison. Si c'est **OOMKilled**, le conteneur dépasse sa limite mémoire, souvent parce que le heap Java est réglé au niveau de la limite sans laisser de place au hors-heap.
Si c'est un **échec de liveness**, la sonde est peut-être mal réglée : un délai initial trop court tue l'application avant qu'elle ait fini de démarrer, c'est précisément le rôle d'une **startup probe**.
Autres causes : variable d'environnement ou secret manquant, dépendance indisponible au démarrage, erreur de configuration.
Le fil : logs, puis événements, puis configuration."

**Q : "Comment tu configures requests et limits ?"**
R : "Je mesure sous charge représentative plutôt que d'estimer.
Les **requests** correspondent à la consommation normale, c'est ce que Kubernetes réserve pour placer le pod. Sous-évaluer expose aux évictions, surévaluer gaspille le cluster.
Les **limits** sont le plafond acceptable. Sur la **mémoire**, dépasser tue le conteneur : il faut une vraie marge, et un heap Java réglé en dessous pour laisser la place au metaspace, aux stacks de threads et aux buffers natifs, environ 25% de marge est une règle courante.
Sur le **CPU**, dépasser ne tue pas mais **throttle** : le service devient lent sans erreur, ce qui est bien plus difficile à diagnostiquer. C'est pourquoi certains recommandent de ne pas fixer de limite CPU du tout, en s'appuyant sur les requests pour le partage équitable.
Et je mettrais un monitoring sur le throttling et les OOMKilled pour ajuster dans le temps plutôt que de figer des valeurs."

**Q : "Déployer une nouvelle version sans aucune coupure."**
R : "Plusieurs briques.
Côté **Kubernetes** : rolling update, avec `maxUnavailable` à zéro si on ne veut aucune perte de capacité, et surtout des **readiness probes** justes, sans elles, le trafic part vers un pod qui n'a pas fini de démarrer et les utilisateurs voient des erreurs.
Côté **arrêt propre** : gestion du SIGTERM pour terminer les requêtes en cours, et un `preStop` avec une courte pause pour laisser le temps au retrait des tables de routage. Sinon on coupe des requêtes en vol.
Côté **stratégie**, si le changement est risqué : du **canary** via Argo Rollouts, une petite part du trafic, observation des métriques d'erreur et de latence, progression par paliers, rollback immédiat sinon.
Et le point le plus négligé : la **base de données**. Pendant le déploiement, les deux versions du code tournent en même temps, donc le schéma doit être compatible avec les deux. D'où les migrations progressives : ajouter avant de supprimer, jamais de changement destructif en une étape."

**Q : "Un pod reste en Pending."**
R : "`kubectl describe pod` en premier : la section Events indique presque toujours la raison. 'Insufficient memory/cpu' : aucun nœud n'a assez de ressources **demandées** (les requests, pas la consommation réelle) ; je réduirais des requests surévaluées ou j'ajouterais des nœuds (l'autoscaler du cluster devrait le faire). 'had taint that the pod didn't tolerate' : il manque une toleration. 'unbound PersistentVolumeClaim' : le stockage n'a pas pu être provisionné (StorageClass, zone). Ou une anti-affinité impossible à satisfaire avec le nombre de nœuds disponibles."

**Q : "Comment rendre un service hautement disponible sur Kubernetes ?"**
R : "Au moins trois replicas, répartis sur plusieurs nœuds et zones par anti-affinité ou `topologySpreadConstraints`, pour qu'une panne de nœud ou de zone ne les emporte pas tous. Un **PodDisruptionBudget** pour que les maintenances ne vident pas le service. Des **readiness probes** justes et un arrêt propre pour les déploiements sans coupure. Des requests réalistes pour un placement fiable, et un HPA pour absorber la charge.
Puis les dépendances : un service n'est pas plus disponible que sa base ou ses appels externes, d'où timeouts, circuit breakers et une base elle-même répliquée."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **CNI** | Container Network Interface : plugin réseau de Kubernetes |
| **CRD** | Custom Resource Definition : nouveau type de ressource Kubernetes |
| **HPA / VPA** | Horizontal / Vertical Pod Autoscaler |
| **PDB** | PodDisruptionBudget |
| **PV / PVC** | PersistentVolume / PersistentVolumeClaim |
| **QoS** | Classes de qualité de service des pods (Guaranteed, Burstable, BestEffort) |
