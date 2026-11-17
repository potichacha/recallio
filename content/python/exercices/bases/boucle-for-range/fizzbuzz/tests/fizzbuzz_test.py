import pytest

from fizzbuzz import fizzbuzz


def test_renvoie_une_liste_vide_pour_zero():
    assert fizzbuzz(0) == []


def test_ecrit_les_nombres_ordinaires_en_chaines():
    assert fizzbuzz(2) == ["1", "2"]


def test_compte_de_1_a_n_inclus():
    assert len(fizzbuzz(7)) == 7


@pytest.mark.parametrize("nombre", [3, 6, 9, 12])
def test_remplace_les_multiples_de_3_par_fizz(nombre):
    assert fizzbuzz(nombre)[nombre - 1] == "Fizz"


@pytest.mark.parametrize("nombre", [5, 10, 20])
def test_remplace_les_multiples_de_5_par_buzz(nombre):
    assert fizzbuzz(nombre)[nombre - 1] == "Buzz"


@pytest.mark.parametrize("nombre", [15, 30, 45])
def test_remplace_les_multiples_de_15_par_fizzbuzz(nombre):
    assert fizzbuzz(nombre)[nombre - 1] == "FizzBuzz"


def test_produit_la_suite_complete_jusqu_a_15():
    assert fizzbuzz(15) == [
        "1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8",
        "Fizz", "Buzz", "11", "Fizz", "13", "14", "FizzBuzz",
    ]
