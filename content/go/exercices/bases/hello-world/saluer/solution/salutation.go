// Package salutation prépare les phrases d'accueil.
package salutation

import "strings"

// Saluer renvoie la phrase d'accueil pour ce prénom.
func Saluer(prenom string) string {
	prenom = strings.TrimSpace(prenom)
	if prenom == "" {
		return "Bonjour !"
	}
	return "Bonjour, " + prenom + " !"
}
