import pytest

from regroupement import inverser, par_initiale


def test_regroupe_les_mots_par_initiale_dans_leur_ordre():
    assert par_initiale(["ane", "bol", "abri"]) == {"a": ["ane", "abri"], "b": ["bol"]}


def test_l_initiale_est_en_minuscule():
    assert par_initiale(["Ours", "orque"]) == {"o": ["Ours", "orque"]}


def test_ignore_les_mots_vides():
    assert par_initiale(["", "chat", ""]) == {"c": ["chat"]}


def test_les_initiales_suivent_l_ordre_de_premiere_apparition():
    assert list(par_initiale(["zèbre", "ane", "zoo"])) == ["z", "a"]


@pytest.mark.parametrize(
    "notes, attendu",
    [
        ({"ana": 15, "bob": 12, "eve": 15}, {15: ["ana", "eve"], 12: ["bob"]}),
        ({"ana": 10}, {10: ["ana"]}),
        ({}, {}),
    ],
)
def test_inverser_regroupe_les_noms_par_note(notes, attendu):
    assert inverser(notes) == attendu


def test_inverser_ne_modifie_pas_le_dictionnaire_recu():
    notes = {"ana": 15}
    inverser(notes)
    assert notes == {"ana": 15}
