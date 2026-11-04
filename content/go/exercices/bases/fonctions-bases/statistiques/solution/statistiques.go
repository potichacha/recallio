// Package statistiques résume une série de notes.
package statistiques

import "errors"

// ErrAucuneNote signale une série vide.
var ErrAucuneNote = errors.New("aucune note")

// Statistiques renvoie la note minimale, la note maximale et la moyenne, ou ErrAucuneNote si la série est vide.
func Statistiques(notes []float64) (minimum, maximum, moyenne float64, err error) {
	if len(notes) == 0 {
		return 0, 0, 0, ErrAucuneNote
	}
	minimum, maximum = notes[0], notes[0]
	somme := 0.0
	for _, n := range notes {
		minimum = min(minimum, n)
		maximum = max(maximum, n)
		somme += n
	}
	return minimum, maximum, somme / float64(len(notes)), nil
}
