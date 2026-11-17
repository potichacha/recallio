import pytest

from classement import classer, podium


def test_classe_par_note_decroissante():
    assert classer([("ana", 12), ("bob", 17), ("eve", 9)]) == ["bob", "ana", "eve"]


def test_departage_les_egalites_par_ordre_alphabetique():
    eleves = [("zoé", 15), ("bob", 12), ("ana", 15), ("max", 15)]
    assert classer(eleves) == ["ana", "max", "zoé", "bob"]


def test_ne_modifie_pas_la_liste_recue():
    eleves = [("eve", 12), ("bob", 17)]
    classer(eleves)
    assert eleves == [("eve", 12), ("bob", 17)]


def test_classe_une_liste_vide():
    assert classer([]) == []


@pytest.mark.parametrize(
    "eleves, attendu",
    [
        ([("ana", 12), ("bob", 17), ("eve", 9), ("léo", 14)], ["bob", "léo", "ana"]),
        ([("ana", 12), ("bob", 17)], ["bob", "ana"]),
        ([], []),
    ],
)
def test_podium_garde_au_plus_les_trois_meilleurs(eleves, attendu):
    assert podium(eleves) == attendu
