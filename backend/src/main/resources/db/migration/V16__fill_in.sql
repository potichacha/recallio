-- Format « compléter le code » (ADR 0027). Les trous d'une version et leurs réponses acceptées sont stockés avec elle, en
-- JSON ([{"accepted": ["hashCode"]}, ...]) : ils changent avec le texte, et une nouvelle version les fige.
-- Rétrocompatible : la colonne est facultative, les autres formats n'ont pas de trous.

ALTER TABLE card DROP CONSTRAINT card_type_check;
ALTER TABLE card ADD CONSTRAINT card_type_check
    CHECK (type IN ('FLASHCARD', 'CODE_OUTPUT', 'OPEN_SCENARIO', 'DEFINITION', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'BUG_FIX',
                    'FILL_IN'));

ALTER TABLE card_version ADD COLUMN blanks jsonb;
ALTER TABLE card_version ADD CONSTRAINT card_version_blanks_is_array
    CHECK (blanks IS NULL OR jsonb_typeof(blanks) = 'array');
