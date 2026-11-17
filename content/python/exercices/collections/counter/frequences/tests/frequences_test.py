import pytest

from frequences import mots_frequents, sont_anagrammes


def test_renvoie_les_mots_les_plus_frequents():
    assert mots_frequents("le chat et le chien et le rat", 2) == ["le", "et"]


def test_compte_les_mots_sans_tenir_compte_des_majuscules():
    assert mots_frequents("Paris paris PARIS Lyon", 1) == ["paris"]


def test_departage_les_egalites_par_ordre_d_apparition():
    assert mots_frequents("b a c a b c", 3) == ["b", "a", "c"]


def test_un_texte_vide_ne_donne_aucun_mot():
    assert mots_frequents("", 3) == []


@pytest.mark.parametrize(
    "a, b, attendu",
    [
        ("chien", "niche", True),
        ("Marie", "aimer", True),
        ("ab c", "c ba", True),
        ("aab", "abb", False),
        ("abc", "abcd", False),
    ],
)
def test_sont_anagrammes(a, b, attendu):
    assert sont_anagrammes(a, b) is attendu
