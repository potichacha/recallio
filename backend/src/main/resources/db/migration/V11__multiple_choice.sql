-- Cartes QCM (CLAUDE.md, section 5.3 ; ADR 0019). Les choix d'une version sont stockés avec elle, en JSON
-- ([{"text": "...", "correct": true}, ...]) : ils changent avec le texte, et une nouvelle version les fige.
-- Rétrocompatible : la colonne est facultative, les autres formats n'ont pas de choix.

ALTER TABLE card DROP CONSTRAINT card_type_check;
ALTER TABLE card ADD CONSTRAINT card_type_check
    CHECK (type IN ('FLASHCARD', 'CODE_OUTPUT', 'OPEN_SCENARIO', 'DEFINITION', 'MULTIPLE_CHOICE'));

ALTER TABLE card_version ADD COLUMN choices jsonb;
ALTER TABLE card_version ADD CONSTRAINT card_version_choices_is_array
    CHECK (choices IS NULL OR jsonb_typeof(choices) = 'array');
