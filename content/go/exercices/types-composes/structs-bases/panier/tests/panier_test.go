package panier

import "testing"

func TestUnPanierVideEstUtilisableEtVautZero(t *testing.T) {
	var p Panier
	if got := p.Total(); got != 0 {
		t.Errorf("Total d'un panier vide = %d, attendu 0", got)
	}
}

func TestLeTotalTientCompteDesQuantites(t *testing.T) {
	var p Panier
	p.Ajouter("pomme", 50, 4)
	p.Ajouter("pain", 120, 1)
	if got := p.Total(); got != 320 {
		t.Errorf("Total = %d, attendu 320", got)
	}
}

func TestAjouterUneReferenceDejaPresenteAugmenteSaQuantite(t *testing.T) {
	var p Panier
	p.Ajouter("pomme", 50, 1)
	p.Ajouter("pomme", 50, 2)
	lignes := p.Lignes()
	if len(lignes) != 1 || lignes[0].Quantite != 3 {
		t.Errorf("Lignes = %v, attendu une ligne de quantité 3", lignes)
	}
}

func TestModifierLesLignesRenvoyeesNeChangePasLePanier(t *testing.T) {
	var p Panier
	p.Ajouter("pomme", 50, 1)
	p.Lignes()[0].Quantite = 100
	if got := p.Total(); got != 50 {
		t.Errorf("Total = %d après modification de la copie, attendu 50", got)
	}
}
