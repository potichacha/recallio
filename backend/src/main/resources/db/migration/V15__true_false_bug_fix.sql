-- Formats vrai ou faux et bug à corriger (ADR 0027). Un vrai ou faux garde ses deux choix fixes (« Vrai », « Faux ») dans
-- card_version.choices, comme un QCM : aucune autre colonne n'est nécessaire.

ALTER TABLE card DROP CONSTRAINT card_type_check;
ALTER TABLE card ADD CONSTRAINT card_type_check
    CHECK (type IN ('FLASHCARD', 'CODE_OUTPUT', 'OPEN_SCENARIO', 'DEFINITION', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'BUG_FIX'));
