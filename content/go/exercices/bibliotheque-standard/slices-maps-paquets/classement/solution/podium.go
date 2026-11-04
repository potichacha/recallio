// Package classement établit le podium d'une partie.
package classement

import (
	"cmp"
	"maps"
	"slices"
)

// Podium renvoie les noms des n meilleurs scores, du meilleur au moins bon, les ex aequo par ordre alphabétique.
func Podium(scores map[string]int, n int) []string {
	if n <= 0 {
		return nil
	}
	noms := slices.Collect(maps.Keys(scores))
	slices.SortFunc(noms, func(a, b string) int {
		return cmp.Or(
			cmp.Compare(scores[b], scores[a]), // le plus grand score d'abord
			cmp.Compare(a, b),                 // puis par nom
		)
	})
	return noms[:min(n, len(noms))]
}
