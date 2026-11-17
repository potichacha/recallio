import pytest

from texte import en_slug, extension, nettoyer_telephone


@pytest.mark.parametrize(
    ("titre", "attendu"),
    [
        ("Apprendre Python", "apprendre-python"),
        ("  Apprendre   Python ", "apprendre-python"),
        ("Les Chaînes en Python", "les-chaînes-en-python"),
        ("Python", "python"),
    ],
)
def test_en_slug_met_en_minuscules_et_relie_les_mots_par_un_tiret(titre, attendu):
    assert en_slug(titre) == attendu


def test_en_slug_d_un_titre_vide_est_vide():
    assert en_slug("   ") == ""


@pytest.mark.parametrize(
    ("nom_fichier", "attendu"),
    [
        ("rapport.pdf", "pdf"),
        ("photo.JPEG", "jpeg"),
        ("archive.tar.gz", "gz"),
    ],
)
def test_extension_renvoie_ce_qui_suit_le_dernier_point_en_minuscules(nom_fichier, attendu):
    assert extension(nom_fichier) == attendu


def test_extension_d_un_nom_sans_point_est_vide():
    assert extension("README") == ""


@pytest.mark.parametrize(
    "numero",
    ["06 12 34 56 78", "06-12-34-56-78", "06.12.34.56.78", "06 12-34.56 78", "0612345678"],
)
def test_nettoyer_telephone_ne_garde_que_les_chiffres(numero):
    assert nettoyer_telephone(numero) == "0612345678"
