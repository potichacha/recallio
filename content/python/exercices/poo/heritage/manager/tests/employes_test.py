from employes import Employe, Manager


def test_un_manager_est_un_employe():
    assert isinstance(Manager("Ana", 60000, 500), Employe)


def test_le_manager_garde_le_nom_et_le_salaire_initialises_par_le_parent():
    ana = Manager("Ana", 60000, 500)
    assert ana.nom == "Ana"
    assert ana.salaire_annuel == 60000
    assert ana.prime_mensuelle == 500


def test_la_paie_d_un_manager_ajoute_sa_prime():
    assert Manager("Ana", 60000, 500).paie_mensuelle() == 5500


def test_la_paie_d_un_employe_ne_change_pas():
    assert Employe("Léo", 36000).paie_mensuelle() == 3000


def test_un_manager_commence_sans_equipe():
    assert Manager("Ana", 60000, 500).equipe == []


def test_chaque_manager_a_sa_propre_equipe():
    ana, max_ = Manager("Ana", 60000, 500), Manager("Max", 54000, 300)
    ana.ajouter(Employe("Léo", 36000))
    assert len(ana.equipe) == 1
    assert max_.equipe == []


def test_la_presentation_heritee_utilise_le_poste_du_manager():
    ana = Manager("Ana", 60000, 500)
    ana.ajouter(Employe("Léo", 36000))
    ana.ajouter(Employe("Zoé", 38000))
    assert ana.poste() == "manager (2)"
    assert ana.presentation() == "Ana, manager (2)"


def test_un_employe_se_presente_avec_son_poste():
    assert Employe("Léo", 36000).presentation() == "Léo, employé"


def test_manager_ne_redefinit_pas_presentation():
    assert "presentation" not in vars(Manager)
