// Package urne compte des votes reçus en parallèle.
package urne

import "sync"

// Urne compte les voix de chaque candidat.
type Urne struct {
	mu   sync.Mutex // protège voix
	voix map[string]int
}

// NouvelleUrne renvoie une urne vide.
func NouvelleUrne() *Urne {
	return &Urne{voix: map[string]int{}}
}

// Voter ajoute une voix au candidat.
func (u *Urne) Voter(candidat string) {
	u.mu.Lock()
	defer u.mu.Unlock()
	u.voix[candidat]++
}

// Voix renvoie les voix du candidat.
func (u *Urne) Voix(candidat string) int {
	u.mu.Lock()
	defer u.mu.Unlock()
	return u.voix[candidat]
}

// Total renvoie le nombre total de voix.
func (u *Urne) Total() int {
	u.mu.Lock()
	defer u.mu.Unlock()
	total := 0
	for _, n := range u.voix {
		total += n
	}
	return total
}
