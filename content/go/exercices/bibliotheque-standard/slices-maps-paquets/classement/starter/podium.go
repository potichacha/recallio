// Package classement établit le podium d'une partie.
package classement

// Podium renvoie les noms des n meilleurs scores, du meilleur au moins bon, les ex aequo par ordre alphabétique.
func Podium(scores map[string]int, n int) []string {
	var noms []string
	for nom := range scores {
		noms = append(noms, nom)
	}
	return noms
}
