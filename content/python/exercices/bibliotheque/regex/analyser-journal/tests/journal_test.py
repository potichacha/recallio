import pytest

from journal import analyser, masquer_emails


def test_analyse_une_ligne_complete():
    assert analyser("2026-11-17 08:15:02 ERROR [paiement] Délai dépassé pour la commande 42") == {
        "date": "2026-11-17",
        "heure": "08:15:02",
        "niveau": "ERROR",
        "module": "paiement",
        "message": "Délai dépassé pour la commande 42",
    }


@pytest.mark.parametrize("niveau", ["DEBUG", "INFO", "WARNING", "ERROR"])
def test_accepte_chacun_des_quatre_niveaux(niveau):
    assert analyser(f"2026-11-17 08:15:02 {niveau} [api] ok")["niveau"] == niveau


def test_le_message_peut_contenir_des_crochets_et_des_chiffres():
    resultat = analyser("2026-01-02 23:59:59 INFO [api_v2] Réponse [200] en 15 ms")
    assert resultat["module"] == "api_v2"
    assert resultat["message"] == "Réponse [200] en 15 ms"


@pytest.mark.parametrize(
    "ligne",
    [
        "",
        "Bonjour",
        "2026-11-17 ERROR [paiement] heure manquante",
        "2026-11-17 08:15:02 FATAL [paiement] niveau inconnu",
        "2026-11-17 08:15:02 error [paiement] niveau en minuscules",
        "2026-11-17 08:15:02 INFO paiement crochets manquants",
        "2026-11-17 08:15:02 INFO [Paiement] module avec une majuscule",
        "2026-11-17 08:15:02 INFO [paiement] ",
        "Copie : 2026-11-17 08:15:02 INFO [paiement] ligne recopiée",
    ],
)
def test_renvoie_none_pour_une_ligne_mal_formee(ligne):
    assert analyser(ligne) is None


def test_masque_chaque_adresse_email():
    texte = "Échec pour ada@exemple.fr, copie à alan.turing+ops@mail.co.uk"
    assert masquer_emails(texte) == "Échec pour <email>, copie à <email>"


def test_laisse_intact_un_texte_sans_adresse():
    assert masquer_emails("Rien à signaler @ 8h, version 2.0") == "Rien à signaler @ 8h, version 2.0"
