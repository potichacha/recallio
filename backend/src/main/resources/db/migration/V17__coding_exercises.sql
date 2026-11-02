-- Exercices de code (ADR 0029). Les fichiers d'une version (code de départ, solution, tests) sont stockés avec elle, en
-- JSON ({"starter": [{"path": ..., "content": ...}], "solution": [...], "tests": [...]}) : ils changent avec l'énoncé,
-- et une nouvelle version les fige. Rétrocompatible : la colonne est facultative, les autres formats n'ont pas de fichiers.

ALTER TABLE card DROP CONSTRAINT card_type_check;
ALTER TABLE card ADD CONSTRAINT card_type_check
    CHECK (type IN ('FLASHCARD', 'CODE_OUTPUT', 'OPEN_SCENARIO', 'DEFINITION', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'BUG_FIX',
                    'FILL_IN', 'CODING_EXERCISE'));

ALTER TABLE card_version ADD COLUMN exercise jsonb;
ALTER TABLE card_version ADD CONSTRAINT card_version_exercise_is_object
    CHECK (exercise IS NULL OR jsonb_typeof(exercise) = 'object');
