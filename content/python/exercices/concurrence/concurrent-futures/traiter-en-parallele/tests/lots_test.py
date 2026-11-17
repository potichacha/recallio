import threading
import time

import pytest

from lots import traiter


def test_renvoie_le_resultat_de_chaque_element():
    resultats, erreurs = traiter([1, 2, 3], lambda n: n * 10)
    assert resultats == {1: 10, 2: 20, 3: 30}
    assert erreurs == {}


def test_garde_les_erreurs_sans_arreter_les_autres_elements():
    def inverse(n):
        return 1 / n

    resultats, erreurs = traiter([2, 0, 4], inverse)
    assert resultats == {2: 0.5, 4: 0.25}
    assert list(erreurs) == [0]
    assert isinstance(erreurs[0], ZeroDivisionError)


def test_appelle_la_fonction_en_parallele():
    # Les trois appels doivent se retrouver ensemble au point de rendez-vous ;
    # un par un, le premier attendrait les deux autres jusqu'au délai de garde.
    rendez_vous = threading.Barrier(3, timeout=2)

    def attendre_les_autres(element):
        rendez_vous.wait()
        return element

    resultats, erreurs = traiter(["a", "b", "c"], attendre_les_autres, max_workers=3)
    assert erreurs == {}
    assert resultats == {"a": "a", "b": "b", "c": "c"}


def test_ne_depasse_pas_max_workers_appels_simultanes():
    verrou = threading.Lock()
    en_cours, maximum = 0, 0

    def compter(element):
        nonlocal en_cours, maximum
        with verrou:
            en_cours += 1
            maximum = max(maximum, en_cours)
        time.sleep(0.02)
        with verrou:
            en_cours -= 1
        return element

    resultats, _ = traiter(range(8), compter, max_workers=2)
    assert len(resultats) == 8
    assert maximum <= 2


def test_une_liste_vide_donne_deux_dictionnaires_vides():
    assert traiter([], str) == ({}, {})


@pytest.mark.parametrize("max_workers", [0, -1])
def test_un_max_workers_nul_ou_negatif_est_refuse(max_workers):
    with pytest.raises(ValueError):
        traiter([1], str, max_workers=max_workers)
