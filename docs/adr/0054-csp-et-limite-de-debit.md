# 0054. Content-Security-Policy et limite de débit, dans nginx

- Statut : accepté
- Date : 2026-11-11
- Prolonge : ADR 0050 (l'essai sans compte), CLAUDE.md section 4.5 (sécurité)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), éléments 14 et 15

## Contexte

L'audit a relevé deux manques avant la bêta.

- nginx envoie quatre en-têtes de sécurité, mais pas de **Content-Security-Policy**. Si un script étranger arrivait
  dans une page (une dépendance compromise, une faille de rendu), rien ne l'empêcherait de s'exécuter ni d'envoyer le
  jeton de l'utilisateur ailleurs.
- Rien ne limite le **débit** des routes ouvertes sans compte : le catalogue, le Tour, les profils publics, et surtout
  l'essai, dont chaque réponse est corrigée par le serveur. Un script peut les appeler sans fin.

## Options envisagées

**Où poser la Content-Security-Policy** :

- **Une balise `<meta>`** dans `index.html` : elle ne couvre ni `frame-ancestors` ni les réponses de l'API.
- **Un en-tête de nginx** (retenu) : avec les autres en-têtes de sécurité, sur chaque réponse.

**Où limiter le débit** :

- **Un filtre dans le backend** : testable avec un `Clock`, mais c'est du code à maintenir, un compteur par instance,
  et la requête a déjà atteint Quarkus.
- **`limit_req` de nginx** (retenu) : aucune dépendance, aucun code, et la requête de trop s'arrête avant le backend.

**Qui compter** :

- **Toute requête, par adresse IP** : une salle de classe ou une entreprise derrière une même adresse serait bridée.
- **Les requêtes sans jeton, par adresse IP** (retenu) : c'est le « sans compte » de l'audit.

## Décision

**La politique**, dans `frontend/nginx/default.conf.template` :

- tout vient de l'application : `default-src 'self'`, `script-src 'self'`, sans script en ligne ;
- une seule origine tierce, le fournisseur OIDC (`connect-src`, `frame-src`), donnée au conteneur par `OIDC_ORIGIN` ;
- trois exceptions, chacune pour une raison :
  - `style-src 'unsafe-inline'` : CodeMirror et les barres de progression écrivent des styles en ligne ;
  - `img-src data: blob:` : les cartes de partage, dessinées dans le navigateur (ADR 0045) ;
  - `font-src data:` : Vite inscrit les petits fichiers de police dans la feuille de style ;
- `object-src 'none'`, `base-uri 'self'`, `form-action 'self'`, `frame-ancestors 'none'`.

**Les limites**, par adresse IP, avec une réponse 429 :

| Routes | Débit | Rafale | Qui est compté |
|---|---|---|---|
| `/api/v1/subjects/*/trial` (essai) | 2 requêtes/s | 10 | les requêtes sans jeton |
| `/api/v1/invitations/*` | 1 requête/s | 5 | toutes : un lien se devine par essais |
| le reste de `/api/` | 20 requêtes/s | 40 | les requêtes sans jeton |

- Les débits sont des variables du conteneur (`RATE_LIMIT_TRIAL`, `RATE_LIMIT_INVITATIONS`, `RATE_LIMIT_PUBLIC`).
- La réponse 429 est un Problem Details (RFC 9457), code `too-many-requests`, avec `Retry-After` : le front affiche
  son détail comme celui d'une erreur du backend.
- Derrière un proxy TLS, nginx lit l'adresse du client dans `X-Forwarded-For`, mais ne croit que le proxy déclaré par
  `TRUSTED_PROXY`. Sans cela, n'importe qui choisirait son adresse.

**Les contrôles** : chaque parcours de bout en bout échoue si le navigateur signale une violation de la politique
(`e2e/tests/support/test.ts`) ; `security.spec.ts` vérifie l'en-tête et le 429 d'une rafale sur l'essai.

## Conséquences

- Une nouvelle origine (Sentry, un CDN) demande une ligne dans la politique ; l'oublier casse un parcours, pas la
  production. C'est ainsi que `font-src data:` a été trouvé.
- `'unsafe-inline'` reste permis pour les styles. Un style en ligne ne lance pas de code ; le retirer demanderait un
  nonce par réponse, donc un rendu côté serveur de `index.html`.
- Une requête qui porte un faux jeton échappe à la limite de nginx. Le backend la refuse en vérifiant sa signature,
  sans toucher la base : le coût est faible, mais il existe. Si la bêta le montre, on comptera aussi ces requêtes.
- Le compteur est en mémoire, par instance de nginx : suffisant pour un serveur, à revoir avec plusieurs.
- **Au déploiement** : régler `OIDC_ORIGIN` sur le Keycloak de production et `TRUSTED_PROXY` sur l'adresse du proxy
  TLS. HSTS reste à poser sur ce proxy (audit, élément 33).
