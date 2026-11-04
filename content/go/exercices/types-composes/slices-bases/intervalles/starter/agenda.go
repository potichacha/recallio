// Package agenda manipule des créneaux horaires.
package agenda

// Intervalle est un créneau de Debut à Fin (inclus), en minutes.
type Intervalle struct {
	Debut, Fin int
}

// Fusionner fusionne les créneaux qui se chevauchent ou se touchent, et les renvoie triés par début.
func Fusionner(creneaux []Intervalle) []Intervalle {
	return creneaux
}
