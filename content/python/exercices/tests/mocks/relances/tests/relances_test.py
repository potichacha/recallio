import datetime
from unittest.mock import Mock, call, patch

import pytest

from relances import relancer

JOUR = datetime.date(2026, 3, 15)


def facture(client, montant, echeance, payee=False):
    return {"client": client, "montant": montant, "echeance": echeance, "payee": payee}


@pytest.fixture
def aujourd_hui():
    with patch("relances.aujourd_hui", return_value=JOUR) as faux:
        yield faux


def test_relance_une_facture_impayee_en_retard(aujourd_hui):
    envoyer = Mock()
    nombre = relancer([facture("ada", 120, datetime.date(2026, 3, 1))], envoyer)
    envoyer.assert_called_once_with("ada", "Rappel : facture de 120 € échue le 2026-03-01")
    assert nombre == 1


def test_ne_relance_ni_les_factures_payees_ni_celles_qui_ne_sont_pas_echues(aujourd_hui):
    envoyer = Mock()
    factures = [
        facture("ada", 50, datetime.date(2026, 3, 1), payee=True),
        facture("linus", 80, datetime.date(2026, 4, 1)),
        facture("grace", 30, JOUR),
    ]
    assert relancer(factures, envoyer) == 0
    envoyer.assert_not_called()


def test_relance_chaque_client_dans_l_ordre_des_factures(aujourd_hui):
    envoyer = Mock()
    factures = [
        facture("ada", 10, datetime.date(2026, 1, 5)),
        facture("linus", 20, datetime.date(2026, 2, 5)),
    ]
    relancer(factures, envoyer)
    assert envoyer.call_args_list == [
        call("ada", "Rappel : facture de 10 € échue le 2026-01-05"),
        call("linus", "Rappel : facture de 20 € échue le 2026-02-05"),
    ]


def test_une_erreur_d_envoi_n_arrete_pas_les_autres_relances(aujourd_hui):
    envoyer = Mock(side_effect=[ConnectionError("serveur absent"), None])
    factures = [
        facture("ada", 10, datetime.date(2026, 1, 5)),
        facture("linus", 20, datetime.date(2026, 2, 5)),
    ]
    assert relancer(factures, envoyer) == 1
    assert envoyer.call_count == 2


def test_la_date_du_jour_vient_de_aujourd_hui(aujourd_hui):
    aujourd_hui.return_value = datetime.date(2026, 1, 1)
    envoyer = Mock()
    assert relancer([facture("ada", 10, datetime.date(2026, 1, 5))], envoyer) == 0
    envoyer.assert_not_called()
    aujourd_hui.assert_called()


def test_sans_facture_rien_n_est_envoye(aujourd_hui):
    envoyer = Mock()
    assert relancer([], envoyer) == 0
    envoyer.assert_not_called()
