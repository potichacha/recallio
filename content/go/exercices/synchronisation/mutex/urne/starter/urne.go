// Package urne compte des votes reçus en parallèle.
package urne

// Urne compte les voix de chaque candidat.
type Urne struct {
	voix map[string]int
}

// NouvelleUrne renvoie une urne vide.
func NouvelleUrne() *Urne {
	return &Urne{voix: map[string]int{}}
}

// Voter ajoute une voix au candidat.
func (u *Urne) Voter(candidat string) {
	u.voix[candidat]++
}

// Voix renvoie les voix du candidat.
func (u *Urne) Voix(candidat string) int {
	return u.voix[candidat]
}

// Total renvoie le nombre total de voix.
func (u *Urne) Total() int {
	return 0
}
