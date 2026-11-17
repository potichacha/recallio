import pytest

from filtres import bornees, carres_des_pairs, initiales


@pytest.mark.parametrize(
    "nombres, attendu",
    [
        ([1, 2, 3, 4], [4, 16]),
        ([-2, 0, 5], [4, 0]),
        ([1, 3], []),
        ([], []),
    ],
)
def test_carres_des_pairs(nombres, attendu):
    assert carres_des_pairs(nombres) == attendu


def test_initiales_en_majuscules():
    assert initiales(["ana", "Bob", "eve"]) == ["A", "B", "E"]


def test_initiales_ignore_les_noms_vides_ou_blancs():
    assert initiales(["", "  ", " léo"]) == ["L"]


def test_bornees_ramene_les_notes_entre_0_et_20():
    assert bornees([-3, 12, 25, 0, 20]) == [0, 12, 20, 0, 20]


def test_bornees_renvoie_une_nouvelle_liste():
    notes = [25]
    assert bornees(notes) == [20]
    assert notes == [25]
