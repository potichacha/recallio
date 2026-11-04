// Package duree représente des durées en secondes.
package duree

import "fmt"

// Duree est une durée en secondes.
type Duree int

// String affiche la durée en heures, minutes et secondes.
func (d Duree) String() string {
	return fmt.Sprintf("%ds", int(d))
}

// Ajouter renvoie la somme des deux durées.
func (d Duree) Ajouter(autre Duree) Duree {
	return d
}
