---
subject: securite
title: Sécurité
category: fundamentals
position: 170
status: published
---

# Sécurité : le document de référence pour les entretiens techniques

> Pour préparer un entretien : sécurité applicative, OWASP, authentification et autorisation.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. SÉCURITÉ
═══════════════════════════════════════════

### Vue d'ensemble

La sécurité applicative repose sur une idée simple mais exigeante : **ne jamais faire confiance aux entrées**, et **ne jamais accorder plus de droits que nécessaire**. La majorité des failles ne viennent pas d'une cryptographie cassée mais de choses banales, une validation absente, un contrôle d'accès oublié, un secret laissé dans un dépôt.

Deux principes structurants. La **défense en profondeur** : plusieurs couches indépendantes, pour qu'une faille unique ne compromette pas tout. Et le **moindre privilège** : chaque composant, chaque compte n'obtient que ce dont il a strictement besoin, ce qui limite les dégâts en cas de compromission.

L'OWASP Top 10 classe les risques web par fréquence et impact. Depuis plusieurs éditions, c'est le **contrôle d'accès défaillant** qui arrive en tête, pas une attaque sophistiquée, simplement une vérification d'autorisation manquante côté serveur.

### Cours

**Authentification** (qui es-tu) vs **autorisation** (qu'as-tu le droit de faire). Deux étapes distinctes, souvent confondues.

**OWASP Top 10, les catégories essentielles** (classement complet de l'édition 2025 plus bas) :
- **Broken Access Control** : un utilisateur accède à des ressources qui ne sont pas les siennes en modifiant un identifiant dans l'URL. La vérification doit être faite **côté serveur, pour chaque ressource**, pas seulement masquée dans l'interface.
- **Cryptographic Failures** : données sensibles en clair, algorithmes obsolètes, absence de HTTPS.
- **Injection** : SQL, commandes système, LDAP. Défense : requêtes **paramétrées**, jamais de concaténation.
- **Insecure Design** : failles de conception, absence de modélisation des menaces.
- **Security Misconfiguration** : endpoints d'administration exposés, valeurs par défaut, stacktraces visibles.

**XSS** : injection de JavaScript dans une page consultée par d'autres. Défense : échapper les sorties, Content-Security-Policy.
**CSRF** : faire exécuter une action à un utilisateur authentifié à son insu. Défense : jeton anti-CSRF, cookies `SameSite`.

**JWT** : jeton signé composé de trois parties encodées en base64 (header, payload, signature). Il est **stateless** : le serveur vérifie la signature sans consulter de session, ce qui facilite le scaling. Points de vigilance : le payload est **lisible par tous** (jamais de secret dedans), la révocation est difficile (d'où des durées de vie courtes + refresh token), et il faut valider l'algorithme côté serveur pour éviter l'attaque `alg: none`.

**OAuth 2.0** est un protocole d'**autorisation déléguée** (autoriser une application à agir en votre nom) ; **OpenID Connect** ajoute la couche d'**authentification** par-dessus.

**RBAC** (par rôles) vs **ABAC** (par attributs : département, heure, propriété de la ressource), plus fin mais plus complexe.

**Mots de passe** : hashés avec un algorithme **lent et salé** (bcrypt, argon2, scrypt). La lenteur est volontaire : elle rend le brute force coûteux. Le sel empêche les rainbow tables.

**Secrets** : dans un gestionnaire dédié (HashiCorp Vault, Secrets Manager cloud), jamais dans le code, une image ou un dépôt. Rotation régulière.

**Entre services** : **mTLS** pour authentifier mutuellement les services (souvent fourni par un service mesh), approche **zero trust** : on ne fait pas confiance à un appel simplement parce qu'il vient du réseau interne.

**Autres mesures** : rate limiting, validation stricte des entrées, HTTPS partout, en-têtes de sécurité (HSTS, CSP, X-Content-Type-Options), pas de stacktrace en production, dépendances scannées et tenues à jour (une grande part des compromissions passe par une librairie vulnérable).

**OWASP Top 10, édition 2025** (remplace l'édition 2021) :
1. **A01 Broken Access Control**, qui absorbe désormais le SSRF.
2. **A02 Security Misconfiguration** (en forte hausse).
3. **A03 Software Supply Chain Failures** : dépendances, outils de build, distribution (nouvelle catégorie, élargit "composants vulnérables").
4. **A04 Cryptographic Failures**.
5. **A05 Injection** (SQL, commandes, LDAP, XSS).
6. **A06 Insecure Design**.
7. **A07 Authentication Failures**.
8. **A08 Software or Data Integrity Failures** (désérialisation non sûre, mises à jour non signées).
9. **A09 Security Logging and Alerting Failures** (l'accent passe sur l'alerte : des logs sans alerte servent peu).
10. **A10 Mishandling of Exceptional Conditions** (nouvelle : erreurs mal gérées, comportements "fail open").

Il existe aussi un **OWASP API Security Top 10**, centré sur les API (autorisation au niveau objet, BOLA, en tête).

**Failles à savoir expliquer** :
- **IDOR / BOLA** : accès à l'objet d'un autre en changeant un identifiant.
- **SSRF** (*Server-Side Request Forgery*) : faire appeler par le serveur une URL choisie par l'attaquant (par exemple l'endpoint de métadonnées du cloud `169.254.169.254` qui délivre des identifiants). Défense : liste blanche de destinations, pas de suivi aveugle des redirections, blocage des plages internes.
- **Mass assignment** : lier directement le JSON reçu à une entité permet de modifier des champs non prévus (`"role": "ADMIN"`). Défense : des DTOs d'entrée explicites.
- **Path traversal** : `../../etc/passwd` dans un nom de fichier. Défense : normaliser le chemin et vérifier qu'il reste sous le répertoire autorisé.
- **Open redirect** : une redirection vers une URL fournie par l'utilisateur, utilisée pour le phishing.
- **XXE**, **désérialisation non sûre**, **ReDoS** (voir les chapitres I/O et chaînes).
- **Clickjacking** : afficher le site dans une iframe piégée ; défense `X-Frame-Options` ou CSP `frame-ancestors`.

**Encodage, hachage, chiffrement, signature** : l'**encodage** (Base64) n'est pas une protection, il est réversible par tous ; le **hachage** est à sens unique (SHA-256 pour l'intégrité, bcrypt/argon2 pour les mots de passe) ; le **chiffrement** est réversible avec une clé (symétrique **AES-GCM**, une même clé, rapide ; asymétrique **RSA/ECC**, clé publique et clé privée, plus lent, utilisé pour l'échange de clés et la signature) ; la **signature** prouve l'origine et l'intégrité (clé privée pour signer, publique pour vérifier) ; le **HMAC** prouve l'intégrité et l'authenticité avec un secret partagé.

**TLS, la poignée de main en bref** (TLS 1.3) : le client propose des algorithmes et une part d'échange de clés, le serveur répond avec la sienne et son certificat ; les deux dérivent une clé de session symétrique (échange de type Diffie-Hellman, qui assure la *forward secrecy*) ; le client vérifie la chaîne de certificats jusqu'à une autorité de confiance et le nom du serveur. Un seul aller-retour en TLS 1.3. Le JDK 27 ajoute un échange de clés hybride **post-quantique** (ML-KEM) pour TLS 1.3. En Java, les certificats de confiance sont dans le **truststore** (`cacerts`), l'identité du serveur dans le **keystore** (formats PKCS12, JKS) ; une erreur `PKIX path building failed` signifie que le certificat (ou une autorité intermédiaire) n'est pas reconnu.

**OAuth 2.0, les flux** :
- **Authorization Code + PKCE** : le flux standard pour toute application avec un utilisateur (web, mobile, SPA) ; PKCE protège l'échange du code contre l'interception.
- **Client Credentials** : service à service, sans utilisateur.
- **Device Code** : appareils sans clavier (télévision).
- **Refresh token** : obtenir un nouveau jeton d'accès sans redemander l'authentification.
- Obsolètes : *Implicit* et *Resource Owner Password* (déconseillés par OAuth 2.1).
Rôles : *resource owner* (l'utilisateur), *client* (l'application), *authorization server* (Keycloak, Auth0...), *resource server* (l'API). **OIDC** ajoute l'**ID token** (identité de l'utilisateur) et l'endpoint userinfo. *Scopes* (ce que le client peut faire) vs rôles (ce que l'utilisateur peut faire).

**JWT, compléments** : algorithmes **HS256** (secret partagé, chaque vérificateur peut aussi signer) vs **RS256/ES256** (clé privée pour signer, publiques distribuées via JWKS : adapté aux microservices). Claims à valider : signature, `exp`, `nbf`, `iss` (émetteur), `aud` (audience, que le jeton est bien destiné à ce service). Jeton **opaque** + introspection : révocation immédiate, mais un appel au serveur d'autorisation par vérification. Côté navigateur, stocker les jetons en cookie `HttpOnly` plutôt qu'en localStorage (exposé au XSS), ou utiliser un BFF qui garde les jetons côté serveur.

**Sessions et cookies** : attributs `HttpOnly` (inaccessible au JavaScript), `Secure` (HTTPS uniquement), `SameSite=Lax/Strict` (limite l'envoi cross-site, protection CSRF), durée limitée, régénération de l'identifiant de session après connexion (contre la fixation de session).

**CORS, détail** : une requête "non simple" (méthode PUT/DELETE, en-tête `Authorization`, JSON) déclenche une requête **preflight** OPTIONS ; le serveur répond avec `Access-Control-Allow-Origin` (jamais `*` avec des identifiants), `-Allow-Methods`, `-Allow-Headers`, `-Allow-Credentials`, `-Max-Age`. CORS ne protège pas le serveur, il protège l'utilisateur du navigateur.

**Rate limiting, algorithmes** : *fixed window* (simple, rafales aux frontières de fenêtres), *sliding window* (plus juste), *token bucket* (autorise des rafales contrôlées), *leaky bucket* (lisse le débit). En Java : Bucket4j, Resilience4j RateLimiter, ou au niveau de la passerelle.

**Crypto en Java** : `SecureRandom` pour tout jeton, `MessageDigest`, `Mac` (HMAC), `Cipher` avec `AES/GCM/NoPadding` (jamais ECB), `KeyStore`, comparaison en temps constant (`MessageDigest.isEqual`) pour les secrets. Ne jamais inventer son propre algorithme.

**Gestion des secrets en pratique** : HashiCorp Vault (secrets dynamiques à durée de vie courte, par exemple des identifiants de base générés à la demande), gestionnaires cloud, injection au runtime, rotation, audit des accès.

**Conformité** : **RGPD** (minimisation, finalité, droit à l'effacement, journal des traitements, chiffrement des données personnelles), PCI-DSS pour les données de carte.

### En situation

- **"Comment sécuriser une API ?"** → checklist en couches.
- **"Authentification entre microservices ?"** → JWT propagé, mTLS, zero trust.
- **"Un utilisateur accède aux données d'un autre"** → broken access control.
- **"Gérer les secrets dans un pipeline ?"** → vault, moindre privilège.

### Questions techniques

**Q : Authentification vs autorisation ?** R : Qui tu es vs ce que tu as le droit de faire.

**Q : Quelle est la faille la plus fréquente selon l'OWASP ?** R : Le contrôle d'accès défaillant.

**Q : Comment se protéger d'une injection SQL ?** R : Requêtes paramétrées, jamais de concaténation.

**Q : XSS vs CSRF ?** R : Injection de script exécuté chez d'autres vs action déclenchée à l'insu d'un utilisateur authentifié.

**Q : Peut-on mettre un secret dans un JWT ?** R : Non, le payload est encodé et lisible.

**Q : Comment révoquer un JWT ?** R : Difficile par nature, d'où des durées courtes, un refresh token, ou une liste de révocation.

**Q : OAuth 2.0 vs OIDC ?** R : Autorisation déléguée vs authentification ajoutée par-dessus.

**Q : Pourquoi bcrypt plutôt que SHA-256 ?** R : Parce qu'il est volontairement lent et salé, ce qui rend le brute force coûteux.

**Q : C'est quoi le zero trust ?** R : Ne pas accorder de confiance en fonction de la position réseau ; authentifier et autoriser chaque appel.

**Q : Le top 3 de l'OWASP 2025 ?** R : Broken Access Control, Security Misconfiguration, Software Supply Chain Failures.

**Q : Quelles nouveautés dans l'OWASP 2025 ?** R : Software Supply Chain Failures et Mishandling of Exceptional Conditions ; le SSRF est intégré au contrôle d'accès.

**Q : C'est quoi une faille SSRF ?** R : Le serveur effectue une requête vers une URL choisie par l'attaquant (services internes, métadonnées cloud).

**Q : C'est quoi le mass assignment ?** R : Lier directement une entrée à un objet interne et permettre la modification de champs non prévus ; défense : DTOs d'entrée.

**Q : Comment se protéger du path traversal ?** R : Normaliser le chemin et vérifier qu'il reste dans le répertoire autorisé.

**Q : Encodage vs hachage vs chiffrement ?** R : Réversible par tous / à sens unique / réversible avec une clé.

**Q : Symétrique vs asymétrique ?** R : Une seule clé partagée, rapide (AES) vs paire publique/privée, plus lente (RSA, ECC).

**Q : C'est quoi un HMAC ?** R : Une empreinte calculée avec un secret partagé, qui prouve intégrité et authenticité.

**Q : Pourquoi AES-GCM plutôt qu'ECB ?** R : ECB chiffre les blocs identiques de la même façon et laisse fuiter des motifs ; GCM chiffre et authentifie.

**Q : Que signifie "PKIX path building failed" ?** R : Le certificat présenté (ou une autorité intermédiaire) n'est pas dans le truststore.

**Q : Keystore vs truststore ?** R : Ma propre identité (clé privée et certificat) vs les certificats auxquels je fais confiance.

**Q : Quel flux OAuth pour une SPA ou une application mobile ?** R : Authorization Code avec PKCE.

**Q : Quel flux OAuth entre deux services ?** R : Client Credentials.

**Q : Pourquoi le flux Implicit est-il obsolète ?** R : Le jeton transite dans l'URL du navigateur, exposé ; PKCE le remplace.

**Q : ID token vs access token ?** R : Identité de l'utilisateur pour le client (OIDC) vs droit d'accès présenté à l'API.

**Q : HS256 vs RS256 ?** R : Secret partagé (tout vérificateur peut signer) vs clé privée pour signer et clés publiques pour vérifier.

**Q : Quels claims valider dans un JWT ?** R : Signature, exp, nbf, iss et aud.

**Q : JWT vs jeton opaque ?** R : Vérifiable localement sans appel mais difficile à révoquer vs révocable immédiatement mais nécessitant une introspection.

**Q : Où stocker un jeton dans un navigateur ?** R : En cookie HttpOnly, Secure, SameSite plutôt qu'en localStorage, ou côté serveur via un BFF.

**Q : Rôle de HttpOnly, Secure et SameSite ?** R : Inaccessible au JavaScript / envoyé uniquement en HTTPS / limite l'envoi cross-site.

**Q : Quand une requête déclenche-t-elle un preflight CORS ?** R : Quand elle n'est pas "simple" : méthode autre que GET/POST/HEAD, en-têtes personnalisés, JSON.

**Q : Peut-on combiner Allow-Origin * et Allow-Credentials ?** R : Non, le navigateur le refuse ; il faut une origine explicite.

**Q : Token bucket vs fixed window ?** R : Rafales contrôlées et débit moyen garanti vs compteur par fenêtre, avec des pics aux frontières.

**Q : Comment comparer deux secrets sans fuite temporelle ?** R : Avec une comparaison en temps constant (MessageDigest.isEqual).

**Q : SAST vs DAST vs SCA ?** R : Analyse du code source / attaque de l'application en fonctionnement / analyse des dépendances.

### Questions de situation

**Q : "Comment tu sécurises une API REST ?"**
R : "Par couches.
**Authentification** : un mécanisme standard (JWT ou OAuth2/OIDC) plutôt qu'une solution maison. En stateless, un jeton signé à durée de vie courte avec refresh token.
**Autorisation**, et c'est le point critique : vérifiée **côté serveur sur chaque endpoint et pour chaque ressource**, pas seulement masquée dans l'interface. La faille la plus fréquente de l'OWASP est le contrôle d'accès défaillant : un utilisateur qui change un identifiant dans l'URL et lit les données d'un autre. Donc je vérifie systématiquement que l'utilisateur authentifié a le droit d'accéder à **cette ressource précise**.
**Validation des entrées** : tout ce qui vient du client est suspect, type, format, bornes. Et des requêtes paramétrées côté base.
**Transport** : HTTPS partout, sans exception.
**Limitation** : rate limiting sur les endpoints sensibles, taille de page bornée côté serveur.
**Exposition** : aucune stacktrace en production ; détail dans les logs, message générique au client.
**Secrets** : dans un vault, jamais dans le code ni dans une image.
Et en continu : scanner les dépendances, parce qu'une part importante des compromissions passe par une librairie vulnérable et non par le code maison."

**Q : "Comment gérer l'authentification entre microservices ?"**
R : "Deux besoins distincts.
L'**identité de l'utilisateur final** doit être propagée dans la chaîne, généralement par un JWT transmis de service en service. Chaque service vérifie la signature localement, sans appeler un serveur d'authentification, pas de point de contention. Le service terminal sait ainsi pour qui il travaille et applique ses règles d'autorisation.
L'**identité du service appelant** est un autre problème : comment le service B sait-il que l'appel vient bien de A et non d'un attaquant présent sur le réseau ? La réponse propre est le **mTLS** : chaque service porte un certificat, l'authentification est mutuelle. Un service mesh comme Istio le fournit de façon transparente, sans le coder dans chaque application.
Le principe de fond est le **zero trust** : ne pas considérer le réseau interne comme sûr.
Et le **moindre privilège** : des comptes de service distincts avec des droits limités, plutôt qu'un compte partagé tout-puissant."

**Q : "Une fonctionnalité permet à l'utilisateur de fournir l'URL d'une image que ton serveur télécharge."**
R : "C'est le cas d'école du **SSRF**. Sans précautions, un attaquant fournit `http://169.254.169.254/...` (les métadonnées du cloud, qui peuvent délivrer des identifiants), `http://localhost:8080/actuator`, ou l'adresse d'un service interne.
Défenses : n'autoriser que `https`, résoudre le nom et **refuser les adresses privées, locales et de lien local** (y compris après redirection, donc soit désactiver les redirections, soit revalider à chaque saut, et se méfier du *DNS rebinding*), une liste blanche de domaines si c'est possible, des limites de taille, de durée et de type de contenu. Idéalement, exécuter les téléchargements depuis un composant isolé sans accès au réseau interne, et activer IMDSv2 côté AWS."

**Q : "Comment tu stockerais des mots de passe utilisateurs ?"**
R : "Jamais en clair, jamais chiffrés de façon réversible, jamais avec un hachage rapide comme SHA-256 seul. Un algorithme **lent, salé et paramétrable** : argon2id ou bcrypt (avec un facteur de coût ajusté pour environ 100 ms à la vérification), scrypt ou PBKDF2 si une conformité l'impose. Le sel unique par mot de passe est généré par l'algorithme et stocké avec le hash.
En Spring, un `DelegatingPasswordEncoder`, qui préfixe l'algorithme et permet de migrer progressivement : à la connexion d'un utilisateur dont le hash utilise un ancien algorithme, on re-hache avec le nouveau.
Autour : limitation des tentatives, vérification contre les listes de mots de passe compromis, MFA, et de préférence déléguer l'authentification à un fournisseur d'identité (Keycloak, OIDC) plutôt que de la gérer soi-même."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **BOLA / IDOR** | Broken Object Level Authorization / Insecure Direct Object Reference : accès à l'objet d'un autre |
| **CORS** | Cross-Origin Resource Sharing : autorisation par le serveur des appels cross-origin dans le navigateur |
| **CSRF** | Cross-Site Request Forgery : action déclenchée à l'insu d'un utilisateur authentifié |
| **CSP** | Content Security Policy : en-tête qui restreint les sources de scripts et contenus |
| **DAST / SAST / SCA** | Tests de sécurité dynamiques / analyse statique du code / analyse des dépendances |
| **HMAC** | Hash-based Message Authentication Code : empreinte authentifiée par un secret |
| **JWT / JWKS** | JSON Web Token / ensemble de clés publiques pour vérifier les JWT |
| **mTLS** | TLS mutuel : authentification des deux parties |
| **OAuth 2.0 / OIDC** | Autorisation déléguée / couche d'authentification par-dessus |
| **OWASP** | Open Worldwide Application Security Project (Top 10 2025) |
| **PKCE** | Proof Key for Code Exchange : protection du flux Authorization Code |
| **RBAC / ABAC** | Contrôle d'accès par rôles / par attributs |
| **SBOM** | Software Bill of Materials : inventaire des composants d'un logiciel |
| **SSRF** | Server-Side Request Forgery |
| **TLS** | Transport Layer Security |
| **XSS** | Cross-Site Scripting : injection de script dans une page |
| **XXE** | XML External Entity : faille des parseurs XML |
