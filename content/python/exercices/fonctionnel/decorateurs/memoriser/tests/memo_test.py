import pytest

from memo import memoriser


def test_renvoie_le_resultat_de_la_fonction():
    @memoriser
    def carre(x):
        return x * x

    assert carre(4) == 16


def test_n_appelle_la_fonction_qu_une_fois_par_argument():
    appels = []

    @memoriser
    def carre(x):
        appels.append(x)
        return x * x

    assert [carre(3), carre(3), carre(3)] == [9, 9, 9]
    assert appels == [3]


def test_distingue_des_arguments_differents():
    appels = []

    @memoriser
    def puissance(base, exposant):
        appels.append((base, exposant))
        return base ** exposant

    assert puissance(2, 3) == 8
    assert puissance(3, 2) == 9
    assert puissance(2, 3) == 8
    assert appels == [(2, 3), (3, 2)]


def test_memorise_aussi_un_resultat_none():
    appels = []

    @memoriser
    def noter(message):
        appels.append(message)

    noter("a")
    noter("a")
    assert appels == ["a"]


def test_chaque_fonction_decoree_a_sa_propre_memoire():
    @memoriser
    def carre(x):
        return x * x

    @memoriser
    def cube(x):
        return x * x * x

    assert carre(3) == 9
    assert cube(3) == 27


def test_memorise_aussi_les_appels_recursifs():
    appels = []

    @memoriser
    def fib(n):
        appels.append(n)
        return n if n < 2 else fib(n - 1) + fib(n - 2)

    assert fib(25) == 75025
    assert len(appels) == 26


def test_une_exception_n_est_pas_memorisee():
    tentatives = []

    @memoriser
    def fragile(x):
        tentatives.append(x)
        if len(tentatives) == 1:
            raise ConnectionError("réseau indisponible")
        return x

    with pytest.raises(ConnectionError):
        fragile(1)
    assert fragile(1) == 1
    assert tentatives == [1, 1]


def test_garde_le_nom_et_la_docstring():
    @memoriser
    def carre(x):
        """Renvoie le carré de x."""
        return x * x

    assert carre.__name__ == "carre"
    assert carre.__doc__ == "Renvoie le carré de x."
