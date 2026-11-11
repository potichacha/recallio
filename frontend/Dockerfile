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
FROM nginx:1.31-alpine
# Applique les correctifs de sécurité Alpine publiés après la construction de l'image de base.
RUN apk upgrade --no-cache
ENV BACKEND_URL=http://backend:8080
# L'origine du fournisseur OIDC, seule origine tierce permise par la Content-Security-Policy (ADR 0054).
ENV OIDC_ORIGIN=http://localhost:8180
# Les débits permis sans compte, par adresse IP (ADR 0054), et le proxy dont on croit l'en-tête X-Forwarded-For.
ENV RATE_LIMIT_PUBLIC=20r/s RATE_LIMIT_TRIAL=2r/s RATE_LIMIT_INVITATIONS=1r/s TRUSTED_PROXY=127.0.0.1
COPY frontend/nginx/security-headers.conf frontend/nginx/api-proxy.conf /etc/nginx/snippets/
COPY frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /repo/frontend/dist /usr/share/nginx/html
EXPOSE 80
