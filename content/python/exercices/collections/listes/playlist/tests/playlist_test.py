import pytest

from playlist import ajouter, deplacer, retirer_tout


def test_ajouter_ajoute_chaque_titre_a_la_fin():
    playlist = ["intro"]
    ajouter(playlist, ["a", "b"])
    assert playlist == ["intro", "a", "b"]


def test_ajouter_modifie_la_liste_recue_et_renvoie_none():
    playlist = []
    assert ajouter(playlist, ["a"]) is None
    assert playlist == ["a"]


def test_deplacer_place_le_titre_a_la_position_demandee():
    playlist = ["a", "b", "c", "d"]
    deplacer(playlist, "d", 1)
    assert playlist == ["a", "d", "b", "c"]


def test_deplacer_en_tete():
    playlist = ["a", "b", "c"]
    deplacer(playlist, "c", 0)
    assert playlist == ["c", "a", "b"]


def test_deplacer_un_titre_absent_leve_value_error_sans_rien_changer():
    playlist = ["a", "b"]
    with pytest.raises(ValueError):
        deplacer(playlist, "z", 0)
    assert playlist == ["a", "b"]


@pytest.mark.parametrize(
    "playlist, titre, attendu, nombre",
    [
        (["a", "b", "a", "a"], "a", ["b"], 3),
        (["a", "b"], "b", ["a"], 1),
        (["a", "b"], "z", ["a", "b"], 0),
        ([], "a", [], 0),
    ],
)
def test_retirer_tout_retire_chaque_occurrence(playlist, titre, attendu, nombre):
    assert retirer_tout(playlist, titre) == nombre
    assert playlist == attendu
