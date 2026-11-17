import dataclasses

import pytest

from commande import Commande, Ligne


def test_une_ligne_a_une_quantite_de_un_par_defaut():
    assert Ligne("pain", 1.5).quantite == 1


def test_le_repr_d_une_ligne_montre_ses_champs():
    assert repr(Ligne("pain", 1.5, 2)) == "Ligne(produit='pain', prix_unitaire=1.5, quantite=2)"


def test_deux_lignes_aux_memes_champs_sont_egales():
    assert Ligne("pain", 1.5, 2) == Ligne("pain", 1.5, 2)
    assert Ligne("pain", 1.5, 2) != Ligne("pain", 1.5, 3)


def test_une_ligne_est_immuable():
    ligne = Ligne("pain", 1.5)
    with pytest.raises(dataclasses.FrozenInstanceError):
        ligne.quantite = 3


def test_une_ligne_est_hachable():
    assert len({Ligne("pain", 1.5), Ligne("pain", 1.5), Ligne("lait", 1.0)}) == 2


@pytest.mark.parametrize("quantite", [0, -2])
def test_une_quantite_inferieure_a_un_est_refusee(quantite):
    with pytest.raises(ValueError):
        Ligne("pain", 1.5, quantite)


def test_un_prix_negatif_est_refuse():
    with pytest.raises(ValueError):
        Ligne("pain", -1.0)


def test_le_total_d_une_ligne():
    assert Ligne("pain", 1.5, 4).total == 6.0


def test_une_commande_neuve_est_vide():
    assert Commande("ana").lignes == []


def test_chaque_commande_a_sa_propre_liste_de_lignes():
    ana, leo = Commande("ana"), Commande("léo")
    ana.ajouter(Ligne("pain", 1.5))
    assert leo.lignes == []


def test_le_total_d_une_commande_additionne_ses_lignes():
    commande = Commande("ana")
    commande.ajouter(Ligne("pain", 1.5, 2))
    commande.ajouter(Ligne("lait", 1.25))
    assert commande.total() == pytest.approx(4.25)


def test_deux_commandes_aux_memes_champs_sont_egales():
    assert Commande("ana", [Ligne("pain", 1.5)]) == Commande("ana", [Ligne("pain", 1.5)])


def test_le_repr_d_une_commande():
    assert repr(Commande("ana")) == "Commande(client='ana', lignes=[])"
