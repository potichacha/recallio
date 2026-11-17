import pytest

from playlist import Playlist


def test_len_compte_les_titres():
    assert len(Playlist("rock", ["Intro", "Solo", "Final"])) == 3


def test_une_playlist_vide_est_fausse_et_une_pleine_vraie():
    assert not Playlist("vide")
    assert Playlist("pleine", ["Intro"])


@pytest.mark.parametrize("indice, attendu", [(0, "Intro"), (1, "Solo"), (-1, "Final")])
def test_un_indice_donne_le_titre(indice, attendu):
    assert Playlist("rock", ["Intro", "Solo", "Final"])[indice] == attendu


def test_un_indice_hors_des_bornes_leve_index_error():
    with pytest.raises(IndexError):
        Playlist("rock", ["Intro"])[5]


def test_une_tranche_renvoie_une_playlist_du_meme_nom():
    extrait = Playlist("rock", ["Intro", "Solo", "Final", "Rappel"])[1:3]
    assert isinstance(extrait, Playlist)
    assert extrait.nom == "rock"
    assert list(extrait) == ["Solo", "Final"]


@pytest.mark.parametrize("titre", ["Solo", "solo", "SOLO"])
def test_in_ignore_la_casse(titre):
    assert titre in Playlist("rock", ["Intro", "Solo"])


def test_un_titre_absent_n_est_pas_dans_la_playlist():
    assert "Rappel" not in Playlist("rock", ["Intro", "Solo"])


def test_on_parcourt_les_titres_dans_l_ordre():
    titres = [titre for titre in Playlist("rock", ["Intro", "Solo", "Final"])]
    assert titres == ["Intro", "Solo", "Final"]


def test_ajouter_allonge_la_playlist():
    p = Playlist("rock")
    p.ajouter("Intro")
    p.ajouter("Solo")
    assert len(p) == 2
    assert p[-1] == "Solo"


def test_chaque_playlist_a_sa_propre_liste():
    a, b = Playlist("a"), Playlist("b")
    a.ajouter("Intro")
    assert len(b) == 0


def test_la_playlist_copie_la_liste_recue():
    titres = ["Intro"]
    p = Playlist("rock", titres)
    titres.append("Solo")
    assert len(p) == 1
