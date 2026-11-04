// Package agenda manipule des créneaux horaires.
package agenda

import (
	"cmp"
	"slices"
)

// Intervalle est un créneau de Debut à Fin (inclus), en minutes.
type Intervalle struct {
	Debut, Fin int
}

// Fusionner fusionne les créneaux qui se chevauchent ou se touchent, et les renvoie triés par début.
func Fusionner(creneaux []Intervalle) []Intervalle {
	tries := slices.Clone(creneaux)
	slices.SortFunc(tries, func(a, b Intervalle) int { return cmp.Compare(a.Debut, b.Debut) })
	res := make([]Intervalle, 0, len(tries))
	for _, c := range tries {
		if len(res) > 0 && c.Debut <= res[len(res)-1].Fin {
			res[len(res)-1].Fin = max(res[len(res)-1].Fin, c.Fin)
			continue
		}
		res = append(res, c)
	}
	return res
}
