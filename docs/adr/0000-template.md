# 0001. Architecture en monolithe modulaire

- Statut : accepté
- Date : 2026-10-18

## Contexte

Recallio est développé par une seule personne, sur son temps libre. Le produit doit
pourtant évoluer par phases : révision solo, puis groupes, mode live et offre entreprise.

La maintenabilité est l'exigence numéro un : le code doit rester compréhensible
par quelqu'un qui le découvre dans six mois. Le budget d'hébergement est minimal,
et il n'y a personne pour opérer une infrastructure complexe.

## Options envisagées

1. **Microservices** (un service par domaine : contenu, révision, gamification…)
   - ✅ Déploiement et montée en charge indépendants
   - ❌ Coût opérationnel élevé : plusieurs déploiements, réseau, observabilité distribuée
   - ❌ Transactions distribuées entre révision et points, cohérence à terme à gérer
   - ❌ Disproportionné pour un seul développeur et quelques utilisateurs

2. **Monolithe en couches classique** (packages globaux `controller/`, `service/`, `repository/`)
   - ✅ Le plus simple à démarrer, structure familière
   - ❌ Les frontières métier n'existent pas : tout service peut appeler tout repository
   - ❌ Avec le temps, le couplage augmente et l'extraction d'un domaine devient impossible

3. **Monolithe modulaire** (découpage par module métier, puis par couche à l'intérieur)
   - ✅ Un seul déploiement, une seule base, des transactions locales simples
   - ✅ Des frontières métier explicites, vérifiables automatiquement
   - ✅ Un module reste extractible plus tard si un vrai besoin apparaît (ex. : `live`)
   - ❌ Demande de la discipline : les règles doivent être outillées, sinon elles s'érodent

## Décision

On choisit le **monolithe modulaire** (option 3).

- Une seule application Quarkus, package racine `com.recallio`.
- Un package par module métier (`content`, `review`, `user`, `gamification`, puis
  `group`, `live`, `organization`), créé uniquement quand sa phase commence.
- À l'intérieur de chaque module : `domain/` (Java pur), `application/`,
  `infrastructure/`, `api/`. Les dépendances vont vers le domaine.
- Un module n'utilise un autre module **que par son API publique** (façade du
  package racine), jamais ses classes internes ni ses tables.
- Ces règles sont vérifiées par des tests **ArchUnit** exécutés à chaque build.
- Un seul module Maven au départ : les frontières sont garanties par ArchUnit,
  pas par le découpage Maven.

## Conséquences

- ➕ Déploiement, tests d'intégration et débogage restent simples (un seul processus).
- ➕ Le domaine sans framework se teste unitairement, sans démarrer Quarkus.
- ➖ Une règle ArchUnit trop stricte peut gêner : toute exception est discutée et
  consignée dans un ADR, jamais contournée en silence.
- ➖ Les modules partagent la même base : l'isolation des tables repose sur la convention
  et sur la revue de code, pas sur des schémas séparés (à reconsidérer si besoin).
- 🔁 Le passage à un build Maven multi-modules (isolation à la compilation) fera
  l'objet d'un nouvel ADR si les frontières par packages s'avèrent insuffisantes.
