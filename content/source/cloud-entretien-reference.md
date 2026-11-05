---
subject: cloud
title: Cloud & Infrastructure as Code
category: cloud-devops
position: 140
status: draft
---

# Cloud & Infrastructure as Code : le document de référence pour les entretiens techniques

> Pour préparer un entretien : cloud et infrastructure as code.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. CLOUD & INFRASTRUCTURE AS CODE
═══════════════════════════════════════════

### Vue d'ensemble

Le cloud transforme l'infrastructure en **service à la demande** : au lieu d'acheter des serveurs, on loue des ressources (calcul, stockage, bases managées, files de messages) facturées à l'usage et créées en quelques minutes par API. Pour un développeur backend, l'enjeu n'est pas de connaître les centaines de services de chaque fournisseur, mais de comprendre les modèles, les briques principales et les compromis : ce qu'on gagne en délégant l'exploitation, ce qu'on perd en contrôle et en portabilité, et ce que ça coûte.

L'**Infrastructure as Code** (IaC) applique au cloud les pratiques du code : l'infrastructure est décrite dans des fichiers versionnés, revue en pull request, appliquée de façon reproductible. Fini les serveurs configurés à la main que personne ne sait reconstruire.

### Cours

**Modèles de service** :
| Modèle | On gère | Le fournisseur gère | Exemples |
|---|---|---|---|
| IaaS | OS, runtime, application | matériel, réseau, virtualisation | machines virtuelles (EC2, Compute Engine) |
| CaaS | conteneurs, application | orchestration | Kubernetes managé (EKS, AKS, GKE) |
| PaaS | application et données | tout le reste | App Service, Cloud Run, Heroku, OpenShift |
| FaaS (serverless) | des fonctions | exécution et scaling, jusqu'à zéro | Lambda, Azure Functions, Cloud Functions |
| SaaS | l'usage | tout | Gmail, Salesforce |

**Modèle de responsabilité partagée** : le fournisseur sécurise *le* cloud (datacenters, matériel, hyperviseur) ; le client sécurise ce qu'il met *dans* le cloud (configuration, identités, données, chiffrement, correctifs de ses machines). La plupart des fuites de données cloud viennent d'erreurs de configuration du client (un bucket public).

**Géographie** : **régions** (zones géographiques indépendantes, choisies pour la latence et la réglementation, souveraineté des données) découpées en **zones de disponibilité** (datacenters séparés avec alimentation et réseau indépendants). Une application hautement disponible se répartit sur plusieurs zones ; la tolérance à la perte d'une région demande une architecture multi-régions, bien plus coûteuse.

**Correspondance des services principaux** :
| Besoin | AWS | Azure | GCP |
|---|---|---|---|
| Machines virtuelles | EC2 | Virtual Machines | Compute Engine |
| Kubernetes managé | EKS | AKS | GKE |
| Conteneurs serverless | Fargate, App Runner | Container Apps | Cloud Run |
| Fonctions | Lambda | Functions | Cloud Functions |
| Stockage objet | S3 | Blob Storage | Cloud Storage |
| Base relationnelle managée | RDS, Aurora | Azure SQL, Database for PostgreSQL | Cloud SQL, AlloyDB |
| NoSQL | DynamoDB | Cosmos DB | Firestore, Bigtable |
| Cache | ElastiCache | Azure Cache for Redis | Memorystore |
| Files et événements | SQS, SNS, EventBridge | Service Bus, Event Grid | Pub/Sub |
| Kafka managé | MSK | Event Hubs (compatible Kafka) | Managed Service for Kafka |
| Identités et droits | IAM | Entra ID, RBAC | IAM |
| Secrets | Secrets Manager | Key Vault | Secret Manager |
| Monitoring | CloudWatch | Monitor | Cloud Monitoring |
| Registry | ECR | ACR | Artifact Registry |
| Réseau privé | VPC | VNet | VPC |
| Équilibreur | ALB (L7), NLB (L4) | Application Gateway, Load Balancer | Cloud Load Balancing |
| CDN | CloudFront | Front Door | Cloud CDN |

**Stockage objet** (S3 et équivalents) : des objets dans des *buckets*, accessibles par HTTP, durabilité très élevée, quasi illimité, peu cher, classes de stockage (fréquent, rare, archive) et règles de cycle de vie. Ce n'est pas un système de fichiers : pas de modification partielle d'un objet. URL **pré-signées** pour laisser un client télécharger ou déposer un fichier directement, sans passer par le backend.

**Réseau cloud** : un **VPC** (réseau privé virtuel) découpé en **sous-réseaux** publics (exposés via une passerelle internet) et privés (bases, services internes, sortie via une passerelle NAT) ; **groupes de sécurité** (pare-feu par ressource, avec état) ; *peering* et *private endpoints* pour accéder aux services managés sans passer par internet ; bastion ou accès par session managée plutôt que SSH ouvert.

**Identités** : rôles IAM attribués aux **ressources** (un pod, une fonction) plutôt que des clés d'accès longue durée dans la configuration ; en Kubernetes, fédération d'identité entre ServiceAccount et rôle cloud (IRSA / Pod Identity sur EKS, Workload Identity sur AKS/GKE). Principe du moindre privilège, politiques explicites.

**Serverless et Java** : Lambda facture à l'invocation et scale à zéro, mais le **démarrage à froid** d'une JVM classique pénalise la latence. Réponses : **SnapStart** (instantané de la JVM initialisée, restauré à l'invocation), compilation **native** GraalVM (Quarkus, Spring Native, Micronaut), frameworks légers, mémoire allouée plus élevée (qui augmente aussi le CPU). Limites : durée d'exécution maximale, pas d'état local, connexions aux bases à gérer (proxy de connexions).

**Coûts (FinOps)** : on paie le calcul au temps, le stockage au volume, et souvent le **trafic sortant** (*egress*), poste fréquemment sous-estimé. Leviers : dimensionnement juste (*right-sizing*), autoscaling, instances réservées ou engagements d'usage pour la charge de base, instances *spot*/préemptibles (très bon marché mais interruptibles) pour le travail tolérant aux interruptions, arrêt des environnements hors heures ouvrées, étiquetage (*tags*) pour attribuer les coûts par équipe.

**Infrastructure as Code** :
- **Terraform** (et son fork open source **OpenTofu**) : langage déclaratif HCL, multi-fournisseurs via des *providers*. Cycle : `terraform init` → `plan` (affiche le différentiel entre le code et la réalité) → `apply`. L'**état** (*state*) mémorise la correspondance entre le code et les ressources réelles : il doit être **distant** (bucket, Terraform Cloud), **verrouillé** pendant un apply (pour éviter deux modifications simultanées) et protégé (il contient des données sensibles). **Modules** réutilisables, *workspaces* ou répertoires par environnement, `terraform import` pour reprendre une ressource existante, détection de **drift** (une modification manuelle) au `plan`.
- **Outils spécifiques** : CloudFormation et CDK (AWS), Bicep (Azure), Pulumi (IaC dans des langages généralistes, y compris Java), Crossplane (infrastructure pilotée par des ressources Kubernetes).
- **Provisionnement vs gestion de configuration** : Terraform crée l'infrastructure ; **Ansible** (sans agent, via SSH, *playbooks* YAML **idempotents**) configure des machines existantes. Avec des conteneurs et des images immuables, la gestion de configuration de serveurs devient moins centrale.
- **Infrastructure immuable** : on ne modifie pas un serveur en place, on en déploie un nouveau à partir d'une image, et on détruit l'ancien. Pas de dérive de configuration, rollback simple.

**Stratégies de migration vers le cloud** (les "R") : *rehost* (lift and shift, déplacer tel quel), *replatform* (petits ajustements, par exemple passer à une base managée), *refactor* (réarchitecturer en cloud-native), *repurchase* (remplacer par un SaaS), *retire*, *retain*.

**Multi-cloud et dépendance** : utiliser des services managés propriétaires accélère mais crée une dépendance (*vendor lock-in*). Kubernetes, les bases open source managées, OpenTelemetry et Terraform limitent cette dépendance, au prix de ne pas profiter de certains services spécifiques. Le multi-cloud "pour la portabilité" a un coût élevé : à justifier par un besoin réel (réglementation, négociation, résilience).

### En situation

- **"Pourquoi une base managée plutôt qu'une base sur une VM ?"** → sauvegardes, correctifs, haute disponibilité, réplicas gérés.
- **"La facture cloud explose"** → trafic sortant, ressources surdimensionnées ou oubliées, absence de tags.
- **"Une clé d'accès AWS a été poussée sur GitHub"** → révoquer immédiatement, rôles IAM plutôt que clés.
- **"Quelqu'un a modifié l'infrastructure à la main"** → drift détecté au plan, réaligner le code.
- **"Lambda en Java trop lente au premier appel"** → démarrage à froid → SnapStart, natif.

### Questions techniques

**Q : IaaS vs PaaS vs SaaS vs FaaS ?** R : On gère l'OS et au-dessus / l'application seule / on utilise un logiciel / on fournit des fonctions exécutées à la demande.

**Q : C'est quoi le modèle de responsabilité partagée ?** R : Le fournisseur sécurise l'infrastructure ; le client sécurise ses configurations, identités et données.

**Q : Région vs zone de disponibilité ?** R : Zone géographique indépendante vs datacenter isolé à l'intérieur d'une région.

**Q : Comment rendre un service résilient à la perte d'un datacenter ?** R : Le répartir sur plusieurs zones de disponibilité, avec des données répliquées.

**Q : C'est quoi une URL pré-signée ?** R : Une URL temporaire qui autorise un accès direct à un objet du stockage sans exposer d'identifiants.

**Q : Sous-réseau public vs privé ?** R : Accessible depuis internet via une passerelle vs sans exposition directe, sortie via NAT.

**Q : Pourquoi préférer des rôles IAM à des clés d'accès ?** R : Des identifiants temporaires attribués à la ressource, sans secret longue durée à stocker ni à faire fuiter.

**Q : Comment réduire le démarrage à froid d'une Lambda Java ?** R : SnapStart, compilation native, frameworks légers, plus de mémoire allouée.

**Q : Quel poste de coût est souvent sous-estimé ?** R : Le trafic réseau sortant.

**Q : Instances spot, pour quoi faire ?** R : Des charges tolérantes aux interruptions (batch, CI), à coût très réduit.

**Q : C'est quoi l'Infrastructure as Code ?** R : Décrire l'infrastructure dans du code versionné, revu et appliqué automatiquement.

**Q : Les commandes principales de Terraform ?** R : init, plan, apply (et destroy, import).

**Q : À quoi sert le state Terraform ?** R : À mémoriser la correspondance entre le code et les ressources réelles ; il doit être distant, verrouillé et protégé.

**Q : C'est quoi le drift ?** R : Un écart entre l'infrastructure réelle et sa description en code, souvent dû à une modification manuelle.

**Q : Terraform vs Ansible ?** R : Provisionner l'infrastructure (déclaratif, état) vs configurer des machines (playbooks idempotents, sans agent).

**Q : C'est quoi l'infrastructure immuable ?** R : Remplacer les serveurs au lieu de les modifier, à partir d'images versionnées.

**Q : Rehost vs refactor ?** R : Déplacer tel quel vs réarchitecturer pour le cloud.

**Q : Avantage et inconvénient des services managés propriétaires ?** R : Rapidité et exploitation déléguée vs dépendance au fournisseur.

### Questions de situation

**Q : "Tu dois déployer une application Spring Boot avec une base PostgreSQL sur AWS. Quelle architecture ?"**
R : "Un VPC sur deux ou trois zones de disponibilité, avec sous-réseaux publics pour l'équilibreur et privés pour l'application et la base. L'application en conteneurs, sur **ECS Fargate** si l'équipe veut éviter Kubernetes, ou **EKS** si elle a déjà la compétence et plusieurs services. Un **ALB** devant, avec HTTPS et un certificat géré.
La base sur **RDS PostgreSQL** en multi-AZ (bascule automatique), avec sauvegardes et restauration à un instant donné ; un réplica de lecture si nécessaire. Les secrets dans **Secrets Manager**, lus par l'application via son rôle IAM, sans clé dans la configuration. Logs et métriques vers CloudWatch ou un collecteur OpenTelemetry.
Le tout décrit en **Terraform**, avec un state distant verrouillé, et déployé par la CI. Je commencerais petit et ajusterais la taille à partir des mesures réelles plutôt que de surdimensionner."

**Q : "Lors d'un terraform plan, on voit qu'une ressource de production va être détruite puis recréée."**
R : "Je n'appliquerais pas avant d'avoir compris. Le plan l'indique avec le symbole `-/+` et l'attribut responsable (*forces replacement*) : souvent un attribut qui ne peut pas être modifié en place (un nom, un sous-réseau, un type de moteur), ou un renommage de ressource dans le code que Terraform interprète comme une suppression suivie d'une création.
Selon le cas : pour un renommage, un bloc `moved` (ou `terraform state mv`) qui dit à Terraform que c'est la même ressource ; pour un attribut immuable, planifier le remplacement avec migration de données ; et protéger les ressources critiques avec `prevent_destroy`. Le plan est revu en pull request précisément pour attraper ce genre de surprise."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **FaaS** | Function as a Service : serverless (Lambda) |
| **IaC** | Infrastructure as Code (Terraform, Ansible) |
| **IaaS, PaaS, SaaS, CaaS** | Modèles de service cloud |
| **IAM** | Identity and Access Management : gestion des identités et droits |
| **RPO / RTO** | Perte de données maximale / durée de restauration maximale |
| **VPC** | Virtual Private Cloud : réseau privé cloud |
