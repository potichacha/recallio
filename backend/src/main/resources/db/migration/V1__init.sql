-- Migration initiale : prouve que Flyway s'exécute au démarrage.
-- Les tables métier arrivent en phase 1 (identifiants UUID v7, voir ADR 0008).
COMMENT ON SCHEMA public IS 'Recallio';
