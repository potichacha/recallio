// Package panier gère le panier d'une boutique.
package panier

import "slices"

// Ligne est un produit du panier, avec son prix unitaire en centimes.
type Ligne struct {
	Ref      string
	Prix     int
	Quantite int
}

// Panier est utilisable à sa valeur zéro.
type Panier struct {
	lignes []Ligne
}

// Ajouter ajoute une ligne, ou augmente la quantité si la référence est déjà présente.
func (p *Panier) Ajouter(ref string, prix, quantite int) {
	for i := range p.lignes {
		if p.lignes[i].Ref == ref {
			p.lignes[i].Quantite += quantite
			return
		}
	}
	p.lignes = append(p.lignes, Ligne{Ref: ref, Prix: prix, Quantite: quantite})
}

// Total renvoie le montant total en centimes.
func (p *Panier) Total() int {
	total := 0
	for _, l := range p.lignes {
		total += l.Prix * l.Quantite
	}
	return total
}

// Lignes renvoie une copie des lignes du panier.
func (p *Panier) Lignes() []Ligne {
	return slices.Clone(p.lignes)
}
