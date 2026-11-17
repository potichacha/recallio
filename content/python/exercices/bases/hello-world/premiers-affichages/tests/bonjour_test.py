import pytest

from bonjour import presenter, saluer


def test_saluer_affiche_bonjour_monde(capsys):
    saluer()
    assert capsys.readouterr().out == "Bonjour, monde !\n"


def test_saluer_ne_renvoie_rien(capsys):
    assert saluer() is None


@pytest.mark.parametrize(
    ("nom", "langage"),
    [("Ada", "Python"), ("Linus", "C"), ("Grace", "COBOL")],
)
def test_presenter_affiche_le_nom_puis_le_langage(capsys, nom, langage):
    presenter(nom, langage)
    assert capsys.readouterr().out == f"Je m'appelle {nom}.\nJ'apprends {langage}.\n"


def test_presenter_affiche_exactement_deux_lignes(capsys):
    presenter("Ada", "Python")
    assert capsys.readouterr().out.count("\n") == 2
