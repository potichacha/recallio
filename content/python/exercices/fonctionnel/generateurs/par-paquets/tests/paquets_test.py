import inspect
from itertools import count, islice

import pytest

from paquets import par_paquets


def test_decoupe_en_paquets_de_la_taille_demandee():
    assert list(par_paquets([1, 2, 3, 4, 5, 6], 2)) == [[1, 2], [3, 4], [5, 6]]


def test_le_dernier_paquet_peut_etre_plus_court():
    assert list(par_paquets("abcde", 2)) == [["a", "b"], ["c", "d"], ["e"]]


def test_une_entree_vide_ne_produit_aucun_paquet():
    assert list(par_paquets([], 3)) == []


def test_est_une_fonction_generatrice():
    assert inspect.isgenerator(par_paquets([1, 2], 1))


def test_fonctionne_sur_un_iterateur_infini():
    assert list(islice(par_paquets(count(), 3), 2)) == [[0, 1, 2], [3, 4, 5]]


def test_ne_lit_que_les_elements_necessaires():
    source = iter(range(10))
    paquets = par_paquets(source, 2)
    assert next(paquets) == [0, 1]
    assert next(source) == 2


def test_chaque_paquet_est_une_liste_distincte():
    premier, second = par_paquets([1, 2, 3, 4], 2)
    assert premier == [1, 2]
    assert second == [3, 4]


@pytest.mark.parametrize("taille", [0, -1])
def test_refuse_une_taille_inferieure_a_un(taille):
    with pytest.raises(ValueError):
        list(par_paquets([1, 2], taille))
