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
FROM nginx:1.29-alpine
# Applique les correctifs de sécurité Alpine publiés après la construction de l'image de base.
RUN apk upgrade --no-cache
ENV BACKEND_URL=http://backend:8080
COPY frontend/nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /repo/frontend/dist /usr/share/nginx/html
EXPOSE 80
