import pytest

from bulletin import mention, moyenne


def test_calcule_la_moyenne_de_plusieurs_notes():
    assert moyenne([12, 8, 16]) == 12


def test_calcule_une_moyenne_qui_n_est_pas_entiere():
    assert moyenne([10, 11, 13]) == pytest.approx(11.333, abs=0.001)


def test_la_moyenne_d_une_seule_note_est_cette_note():
    assert moyenne([17]) == 17


def test_renvoie_none_sans_aucune_note():
    assert moyenne([]) is None


@pytest.mark.parametrize(
    ("valeur", "attendue"),
    [
        (18, "très bien"),
        (16, "très bien"),
        (15.5, "bien"),
        (14, "bien"),
        (12, "assez bien"),
        (10, "passable"),
        (9.99, "insuffisant"),
        (0, "insuffisant"),
    ],
)
def test_donne_la_mention_de_chaque_tranche(valeur, attendue):
    assert mention(valeur) == attendue


def test_ne_donne_aucune_mention_sans_moyenne():
    assert mention(None) is None


def test_enchaine_la_moyenne_et_la_mention():
    assert mention(moyenne([12, 15, 18])) == "bien"
