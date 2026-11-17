import pytest

from saisies import lire_entier, moyenne, somme_valide


@pytest.mark.parametrize("texte, attendu", [("42", 42), ("-7", -7), (" 8 ", 8)])
def test_lire_entier_convertit_un_texte_valide(texte, attendu):
    assert lire_entier(texte) == attendu


@pytest.mark.parametrize("texte", ["abc", "3.5", "", "4,5"])
def test_lire_entier_renvoie_none_pour_un_texte_invalide(texte):
    assert lire_entier(texte) is None


def test_lire_entier_renvoie_la_valeur_par_defaut_demandee():
    assert lire_entier("abc", 0) == 0


def test_lire_entier_accepte_none_sans_lever_d_exception():
    assert lire_entier(None, -1) == -1


def test_somme_valide_additionne_les_entiers_et_liste_les_rejets_dans_l_ordre():
    assert somme_valide(["10", "vingt", "30", "4,5"]) == (40, ["vingt", "4,5"])


def test_somme_valide_d_une_liste_vide():
    assert somme_valide([]) == (0, [])


def test_somme_valide_sans_aucun_rejet():
    assert somme_valide(["1", "-2", " 3 "]) == (2, [])


def test_moyenne_ignore_les_saisies_invalides():
    assert moyenne(["12", "abc", "16"]) == 14


def test_moyenne_sans_saisie_valide_renvoie_none():
    assert moyenne(["abc", "?"]) is None


def test_moyenne_d_une_liste_vide_renvoie_none():
    assert moyenne([]) is None
