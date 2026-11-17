import pytest

from transaction import Transaction, ignorer


def test_les_changements_sont_gardes_quand_le_bloc_reussit():
    stock = {"pommes": 5}
    with Transaction(stock) as brouillon:
        brouillon["pommes"] = 3
        brouillon["poires"] = 2
    assert stock == {"pommes": 3, "poires": 2}


def test_le_bloc_travaille_sur_une_copie():
    stock = {"pommes": 5}
    with Transaction(stock) as brouillon:
        brouillon["pommes"] = 0
        assert stock == {"pommes": 5}
        assert brouillon is not stock


def test_une_cle_supprimee_dans_le_bloc_disparait():
    stock = {"pommes": 5, "poires": 1}
    with Transaction(stock) as brouillon:
        del brouillon["poires"]
    assert stock == {"pommes": 5}


def test_le_dictionnaire_reste_le_meme_objet():
    stock = {"pommes": 5}
    meme_stock = stock
    with Transaction(stock) as brouillon:
        brouillon["kiwis"] = 1
    assert meme_stock == {"pommes": 5, "kiwis": 1}


def test_une_exception_annule_tous_les_changements():
    stock = {"pommes": 5}
    with pytest.raises(ValueError):
        with Transaction(stock) as brouillon:
            brouillon["pommes"] = -1
            brouillon["poires"] = 2
            raise ValueError("stock négatif")
    assert stock == {"pommes": 5}


def test_l_exception_remonte_a_l_appelant():
    with pytest.raises(KeyError):
        with Transaction({}) as brouillon:
            brouillon["absente"]


def test_ignorer_supprime_l_exception_nommee():
    with ignorer(KeyError):
        {}["absente"]


def test_ignorer_arrete_le_bloc_a_l_exception():
    etapes = []
    with ignorer(ValueError):
        etapes.append("avant")
        int("x")
        etapes.append("après")
    assert etapes == ["avant"]


@pytest.mark.parametrize("erreur", [KeyError, IndexError])
def test_ignorer_accepte_plusieurs_types(erreur):
    with ignorer(KeyError, IndexError):
        raise erreur


def test_ignorer_laisse_remonter_les_autres_exceptions():
    with pytest.raises(ZeroDivisionError):
        with ignorer(KeyError):
            1 / 0


def test_ignorer_ne_change_rien_sans_exception():
    with ignorer(KeyError):
        resultat = 1 + 1
    assert resultat == 2
