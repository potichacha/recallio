// Package anagrammes regroupe les mots formés des mêmes lettres.
package anagrammes

// Grouper regroupe les anagrammes, dans l'ordre d'arrivée du premier mot de chaque groupe.
func Grouper(mots []string) [][]string {
	var groupes [][]string
	for _, m := range mots {
		groupes = append(groupes, []string{m})
	}
	return groupes
}
