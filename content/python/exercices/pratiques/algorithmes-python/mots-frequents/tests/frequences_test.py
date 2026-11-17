import pytest

from frequences import plus_frequents


def test_renvoie_les_mots_du_plus_au_moins_frequent():
    mots = ["chat", "chien", "chat", "souris", "chien", "chat"]
    assert plus_frequents(mots, 2) == ["chat", "chien"]


def test_departage_les_egalites_par_ordre_alphabetique():
    mots = ["zebre", "lion", "ane", "zebre", "lion", "ane"]
    assert plus_frequents(mots, 2) == ["ane", "lion"]


def test_l_ordre_alphabetique_ne_passe_qu_apres_la_frequence():
    mots = ["b", "a", "b", "c", "c", "c"]
    assert plus_frequents(mots, 3) == ["c", "b", "a"]


def test_ignore_la_casse_et_renvoie_des_minuscules():
    assert plus_frequents(["Python", "python", "Go", "PYTHON"], 1) == ["python"]


def test_k_plus_grand_que_le_nombre_de_mots_distincts_renvoie_tout():
    assert plus_frequents(["a", "b", "a"], 10) == ["a", "b"]


@pytest.mark.parametrize("k", [0, -3])
def test_k_nul_ou_negatif_renvoie_une_liste_vide(k):
    assert plus_frequents(["a", "b", "c", "d"], k) == []


def test_sans_mot_renvoie_une_liste_vide():
    assert plus_frequents([], 3) == []


def test_ne_modifie_pas_la_liste_recue():
    mots = ["b", "A", "b"]
    plus_frequents(mots, 1)
    assert mots == ["b", "A", "b"]


def test_tient_sur_une_grande_liste():
    mots = [f"mot{i % 1000}" for i in range(100_000)] + ["mot7"]
    assert plus_frequents(mots, 2) == ["mot7", "mot0"]
