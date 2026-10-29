# recallio-frontend

PWA React + TypeScript + Vite. Voir le [README racine](../README.md).

```shell
npm install
npm run dev            # http://localhost:5173, proxy /api vers le backend (localhost:8080)
npm test               # Vitest + Testing Library
npm run lint           # ESLint
npm run format:check   # Prettier
npm run build          # génère le client API, vérifie les types, construit dist/
```

Le client API (`src/api/schema.d.ts`) est **généré** depuis la spec OpenAPI du backend
(`npm run generate:api`, lancé automatiquement avant `dev`, `build` et `typecheck`). Il n'est
pas versionné et ne doit jamais être modifié à la main.
