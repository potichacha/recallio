// Package duree représente des durées en secondes.
package duree

import "fmt"

// Duree est une durée en secondes.
type Duree int

// String affiche la durée en heures, minutes et secondes.
func (d Duree) String() string {
	signe := ""
	if d < 0 {
		signe, d = "-", -d
	}
	h, m, s := int(d)/3600, int(d)%3600/60, int(d)%60
	switch {
	case h > 0:
		return fmt.Sprintf("%s%dh%02dm%02ds", signe, h, m, s)
	case m > 0:
		return fmt.Sprintf("%s%dm%02ds", signe, m, s)
	default:
		return fmt.Sprintf("%s%ds", signe, s)
	}
}

// Ajouter renvoie la somme des deux durées.
func (d Duree) Ajouter(autre Duree) Duree {
	return d + autre
}
