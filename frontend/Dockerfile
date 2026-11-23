# syntax=docker/dockerfile:1
# Contexte de build : la racine du dépôt (le client API est généré depuis la spec du backend).

# --- Build : fichiers statiques ------------------------------------------
FROM node:24-alpine AS build
WORKDIR /repo/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY backend/src/main/openapi/openapi.yaml /repo/backend/src/main/openapi/openapi.yaml
COPY frontend/ ./
RUN npm run build

# --- Exécution : nginx sert la PWA et relaie /api vers le backend --------
FROM nginx:1.31-alpine AS runtime
# Applique les correctifs de sécurité Alpine publiés après la construction de l'image de base. La CI ne met jamais
# cette étape en cache (no-cache-filters: runtime, ADR 0070) : chaque image prend les derniers.
RUN apk upgrade --no-cache
ENV BACKEND_URL=http://backend:8080
# L'adresse du fournisseur OIDC (son royaume, chez Keycloak) : nginx la met dans index.html, où la PWA la lit à son
# démarrage. Une seule image sert ainsi tous les environnements (ADR 0069).
ENV OIDC_AUTHORITY=http://localhost:8180/realms/recallio
# L'origine du fournisseur OIDC, seule origine tierce permise par la Content-Security-Policy (ADR 0054). Une variable à
# part, à garder accordée à OIDC_AUTHORITY : nginx ne sait pas en déduire l'origine.
ENV OIDC_ORIGIN=http://localhost:8180
# L'origine publique du site, sans barre finale (https://recallio.fr, par exemple) : nginx la met dans les balises de
# partage et dans robots.txt, dont les adresses doivent être absolues (ADR 0065). À donner aussi au backend.
ENV PUBLIC_ORIGIN=http://localhost:3000
# Les débits permis sans compte, par adresse IP (ADR 0054), et le proxy dont on croit l'en-tête X-Forwarded-For.
ENV RATE_LIMIT_PUBLIC=20r/s RATE_LIMIT_TRIAL=2r/s RATE_LIMIT_INVITATIONS=1r/s TRUSTED_PROXY=127.0.0.1
# Les erreurs de la PWA dans Sentry (ADR 0070) : nginx met le DSN, l'environnement et la version dans index.html, où la
# PWA les lit à son démarrage. Sans DSN, elle ne charge rien et n'envoie rien. SENTRY_ORIGIN, l'origine du DSN, entre
# dans la Content-Security-Policy : à accorder au DSN, comme OIDC_ORIGIN à OIDC_AUTHORITY.
ENV SENTRY_DSN="" SENTRY_ENVIRONMENT=production SENTRY_RELEASE="" SENTRY_ORIGIN=""
COPY frontend/nginx/security-headers.conf frontend/nginx/api-proxy.conf /etc/nginx/snippets/
COPY frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /repo/frontend/dist /usr/share/nginx/html
EXPOSE 80
