-- Les pages du Tour (ADR 0037) : chaque notion garde les siennes, en JSON ([{"title": ..., "text": ..., "example":
-- {"files": [...], "output": ...}, "task": {...}}]). Elles changent avec la notion et se réimportent avec elle ; la
-- progression du Tour se rattache à la notion, pas à une page. Rétrocompatible : une notion sans page a une liste vide.

ALTER TABLE notion ADD COLUMN tour jsonb NOT NULL DEFAULT '[]';
ALTER TABLE notion ADD CONSTRAINT notion_tour_is_array CHECK (jsonb_typeof(tour) = 'array');
