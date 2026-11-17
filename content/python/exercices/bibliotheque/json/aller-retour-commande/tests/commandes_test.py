import json
from datetime import date

import pytest

from commandes import deserialiser, serialiser

COMMANDE = {"id": 7, "date": date(2026, 11, 17), "client": "Zoé", "tags": {"urgent", "cadeau"}}


def test_serialise_la_date_au_format_iso():
    assert json.loads(serialiser(COMMANDE))["date"] == "2026-11-17"


def test_serialise_les_tags_en_liste_triee():
    assert json.loads(serialiser(COMMANDE))["tags"] == ["cadeau", "urgent"]


def test_trie_les_cles_et_garde_les_accents():
    attendu = '{"client": "Zoé", "date": "2026-11-17", "id": 7, "tags": ["cadeau", "urgent"]}'
    assert serialiser(COMMANDE) == attendu


def test_ne_modifie_pas_la_commande_recue():
    commande = dict(COMMANDE)
    serialiser(commande)
    assert commande == COMMANDE


def test_l_aller_retour_redonne_la_commande():
    assert deserialiser(serialiser(COMMANDE)) == COMMANDE


def test_deserialiser_redonne_une_vraie_date_et_un_ensemble():
    commande = deserialiser('{"id": 1, "date": "2026-01-31", "client": "Ada", "tags": ["a", "a", "b"]}')
    assert commande["date"] == date(2026, 1, 31)
    assert commande["tags"] == {"a", "b"}


@pytest.mark.parametrize("texte", ["{'id': 1}", "pas du json", '{"id": 1,}'])
def test_deserialiser_refuse_un_json_invalide(texte):
    with pytest.raises(ValueError):
        deserialiser(texte)


def test_deserialiser_refuse_une_commande_sans_identifiant():
    with pytest.raises(ValueError):
        deserialiser('{"date": "2026-11-17", "client": "Ada", "tags": []}')
