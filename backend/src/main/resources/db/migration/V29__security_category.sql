-- La catégorie Cybersécurité (ADR 0052) : la liste fermée des catégories (CLAUDE.md, section 2.5) gagne une valeur.
-- La contrainte d'origine (V6) porte le nom que PostgreSQL donne à une contrainte de colonne.
ALTER TABLE subject DROP CONSTRAINT subject_category_check;
ALTER TABLE subject ADD CONSTRAINT subject_category_check
    CHECK (category IN ('LANGUAGES', 'FRAMEWORKS', 'CLOUD_DEVOPS', 'DATA', 'SECURITY', 'FUNDAMENTALS', 'AI'));
