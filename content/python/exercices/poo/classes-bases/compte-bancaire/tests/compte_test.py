import pytest

from compte import Compte


def test_un_compte_neuf_a_un_solde_nul():
    compte = Compte("Ana")
    assert compte.titulaire == "Ana"
    assert compte.solde == 0


def test_un_compte_peut_partir_d_un_solde_de_depart():
    assert Compte("Ana", 100).solde == 100


def test_deposer_augmente_le_solde():
    compte = Compte("Ana", 10)
    compte.deposer(50)
    assert compte.solde == 60


def test_retirer_diminue_le_solde():
    compte = Compte("Ana", 100)
    compte.retirer(30)
    assert compte.solde == 70


@pytest.mark.parametrize("operation", ["deposer", "retirer"])
@pytest.mark.parametrize("montant", [0, -5])
def test_un_montant_nul_ou_negatif_est_refuse(operation, montant):
    compte = Compte("Ana", 100)
    with pytest.raises(ValueError):
        getattr(compte, operation)(montant)
    assert compte.solde == 100


def test_un_retrait_plus_grand_que_le_solde_est_refuse_sans_rien_changer():
    compte = Compte("Ana", 40)
    with pytest.raises(ValueError):
        compte.retirer(50)
    assert compte.solde == 40
    assert compte.operations == []


def test_les_operations_reussies_sont_gardees_dans_l_ordre():
    compte = Compte("Ana")
    compte.deposer(50)
    compte.retirer(20)
    assert compte.operations == [("dépôt", 50), ("retrait", 20)]


def test_chaque_compte_a_son_propre_historique():
    ana, leo = Compte("Ana"), Compte("Léo")
    ana.deposer(10)
    assert leo.operations == []
    assert ana.operations == [("dépôt", 10)]
