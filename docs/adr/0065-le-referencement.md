# 0065. Le référencement : des balises de partage fixes, robots.txt, et un plan du site produit par le backend

- Statut : accepté
- Date : 2026-11-20
- Prolonge : ADR 0043 (profil public), ADR 0054 (limite de débit), ADR 0058 (identité « Rico »), ADR 0064 (parcours
  mesuré)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 40

## Contexte

Avant la bêta, Recallio doit pouvoir être trouvé et partagé :

- un lien vers Recallio partagé sur WhatsApp, LinkedIn ou Slack doit montrer un aperçu (un titre, une description, une
  image) ; il ne montrait que « Recallio » ;
- les moteurs de recherche doivent trouver les pages publiques utiles, les leçons et les pages du Tour des sujets
  publiés (765 pages pour Java et Go), sans indexer ce qui est privé.

Trois contraintes :

- **la PWA est une application monopage** : nginx sert le même `index.html` à toutes les adresses, et le contenu arrive
  par l'API, rendu en JavaScript. Google et Bing exécutent le JavaScript ; les robots des aperçus de lien, non : ils
  lisent le HTML tel que nginx le sert ;
- **seul le backend sait ce qui est publié** : un sujet est publié quand il a été importé en `published`, et c'est la
  base qui le dit (CLAUDE.md, section 2.5) ;
- **les adresses doivent être absolues** dans un aperçu (l'image), dans un plan du site et dans la ligne `Sitemap:` de
  `robots.txt`. Ni le front ni le backend ne connaissaient l'origine publique du site.

## Options envisagées

**Le rendu des pages** :

- **Un rendu côté serveur, ou un prérendu des pages publiques** : un HTML complet et des balises propres à chaque page.
  Mais un serveur Node de plus à héberger, ou un build qui demande le backend et le contenu pour produire quelque 800
  pages, à refaire à chaque import.
- **Des balises propres à chaque page, injectées par le serveur** (le titre d'un sujet ou le nom d'un profil dans
  l'aperçu) : nginx devrait interroger le backend pour chaque page qu'il sert. Écarté pour l'instant, à revoir si les
  liens partagés le demandent.
- **Des balises fixes, et le rendu JavaScript** (retenu) : les mêmes balises de partage pour toutes les pages ; les
  moteurs rendent les pages eux-mêmes, et chacune a déjà son titre d'onglet (`usePageTitle`, ADR 0058).

**Le plan du site** :

- **Un fichier produit au build depuis `content/`** : un second lecteur du contenu, en JavaScript, à côté du parseur
  Java ; et le statut publié vit dans la base, qu'un fichier du dépôt peut devancer.
- **Produit par le backend** (retenu), à partir du catalogue publié.

**L'origine publique** :

- **Déduite des en-têtes du proxy** (`Host`, `X-Forwarded-Host`, `X-Forwarded-Proto`) : elle dépend de toute la chaîne
  de proxys, et un en-tête mal relayé, ou choisi par le client, donnerait des adresses fausses.
- **Une variable explicite, `PUBLIC_ORIGIN`** (retenu), donnée aux deux conteneurs.

## Décision

1. **Les balises de partage**, dans `frontend/index.html`, les mêmes pour toutes les pages : `og:type` (`website`),
   `og:site_name`, `og:locale` (`fr_FR`), `og:title`, `og:description`, `og:image`, sa largeur, sa hauteur et son
   texte de remplacement, et `twitter:card` (`summary_large_image`).
   - **Ni `og:url` ni `<link rel="canonical">`** : fixes, ils diraient que toutes les pages sont la même, et un moteur
     ne garderait que l'accueil.
   - L'image, `frontend/public/og-image.png` (1200 × 630, 62 Ko), est dans l'identité « Rico » : Rico, « Recallio » et
     « Révise tes entretiens techniques, un peu chaque jour », aux couleurs du thème clair, sans dégradé ni émoji. Sa
     source est `frontend/scripts/og-image.html`, rendue par un navigateur sans fenêtre (la commande est dans le
     fichier). Le service worker ne la met pas en cache.
   - **Une page introuvable** pose `<meta name="robots" content="noindex">` tant qu'elle est affichée
     (`useNoIndex`) : l'application répond 200 à toute adresse, et un moteur indexerait sinon la page 404.
2. **`frontend/public/robots.txt`** :
   - il écarte les pages réservées aux comptes (les enfants de `RequireAuth` dans `routes.tsx`), la connexion
     (`/auth/`), l'essai (`/try/`, interactif), les profils publics (`/u/` : rendus publics pour être partagés, pas
     pour être trouvés par un moteur, ADR 0043) et la visite du parcours mesuré (`/api/v1/funnel/`, ADR 0064 : un robot
     qui exécute le JavaScript de la page publique ne doit pas la compter) ;
   - il n'écarte **jamais `/api/` en entier** : un moteur en a besoin pour afficher les pages qu'il rend ;
   - il renvoie au plan du site (`Sitemap:`) ;
   - `frontend/src/seo.test.ts` lit chaque route de `routes.tsx` contre ce fichier, avec les jokers `*` et `$` de la
     RFC 9309 : une page de compte ajoutée sans sa règle fait échouer `npm test`.
3. **Le plan du site**, `GET /api/v1/sitemap.xml`, public, dans le module `content` :
   - `SitemapService` donne les chemins : l'accueil, `/learn`, `/tours` et `/subjects`, puis, pour chaque sujet publié
     (jamais un brouillon, même annoncé), sa page, ses leçons (les notions qui en ont une) et, s'il a un Tour, son
     sommaire et ses pages. Ni les thèmes, ni les profils, ni l'essai. Ces chemins suivent `frontend/src/routes.tsx` ;
   - `SitemapResource` écrit le XML avec `XMLStreamWriter`, du JDK, qui échappe ce qu'il faut. Chaque page n'a que son
     adresse (`<loc>`) : Google ignore `changefreq` et `priority`, et **aucune date n'est fiable pour `lastmod`** : les
     dates de la base sont celles des imports, pas celles des changements d'une page (une leçon montre aussi ses
     voisines, la page d'un sujet ses comptes), et Google ne croit plus un `lastmod` qui se trompe ;
   - `Cache-Control: public, max-age=3600` ;
   - nginx le sert sous `/sitemap.xml` (`location = /sitemap.xml`), compté par la zone `public` de la limite de débit
     (ADR 0054). Le serveur de dev de Vite fait de même, et le service worker ne sert la PWA ni à la place de
     `/robots.txt` ni à celle de `/sitemap.xml`.
4. **L'origine publique**, `PUBLIC_ORIGIN` (par exemple `https://recallio.fr`, sans barre finale) :
   - nginx remplace le marqueur `__PUBLIC_ORIGIN__` d'`index.html` et de `robots.txt` (`sub_filter`), seulement dans
     la `location` qui sert les fichiers, jamais sur les réponses de l'API. Le `Dockerfile` du front lui donne la valeur
     du compose, `http://localhost:3000` ;
   - le backend la lit dans `recallio.public-origin`, obligatoire en production (le démarrage échoue sans elle), et en
     retire une barre finale. En dev, l'origine de Vite.

## Conséquences

- ➕ Un lien partagé montre un aperçu, et les moteurs trouvent les pages publiques, sans serveur de plus ni étape de
  build. Le parcours `e2e/tests/seo.spec.ts` le vérifie sur la pile du compose.
- ➕ Un sujet publié entre dans le plan du site à son import ; un brouillon n'y entre pas.
- ➖ L'aperçu est le même pour toutes les pages : un lien vers un profil public, une leçon ou un sujet montre la carte
  générique de Recallio.
- ➖ Les profils publics ne sont ni dans le plan ni explorés : qui veut être trouvé partage son lien lui-même.
- ➖ Le référencement repose sur le rendu JavaScript des moteurs : Google et Bing le font, avec un délai ; un moteur
  qui ne le fait pas ne voit qu'une page vide.
- ➖ Les chemins du plan du site recopient ceux de `routes.tsx` : une adresse qui change dans la PWA change aussi dans
  `SitemapService`. Un commentaire le rappelle des deux côtés.
- 🔁 **Au déploiement** : donner `PUBLIC_ORIGIN` aux deux conteneurs, `frontend` et `backend`, avec la même valeur, puis
  déclarer le plan du site dans Google Search Console et Bing Webmaster Tools.
