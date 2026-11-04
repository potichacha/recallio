// Package anagrammes regroupe les mots formés des mêmes lettres.
package anagrammes

import (
	"slices"
	"strings"
)

// Grouper regroupe les anagrammes, dans l'ordre d'arrivée du premier mot de chaque groupe.
func Grouper(mots []string) [][]string {
	indices := map[string]int{}
	var groupes [][]string
	for _, m := range mots {
		k := cle(m)
		i, ok := indices[k]
		if !ok {
			i = len(groupes)
			indices[k] = i
			groupes = append(groupes, nil)
		}
		groupes[i] = append(groupes[i], m)
	}
	return groupes
}

// cle renvoie les lettres du mot, en minuscules et triées : deux anagrammes ont la même.
func cle(mot string) string {
	lettres := []rune(strings.ToLower(mot))
	slices.Sort(lettres)
	return string(lettres)
}
