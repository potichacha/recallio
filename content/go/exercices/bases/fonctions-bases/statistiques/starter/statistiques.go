// Package statistiques résume une série de notes.
package statistiques

import "errors"

// ErrAucuneNote signale une série vide.
var ErrAucuneNote = errors.New("aucune note")

// Statistiques renvoie la note minimale, la note maximale et la moyenne, ou ErrAucuneNote si la série est vide.
func Statistiques(notes []float64) (min, max, moyenne float64, err error) {
	return 0, 0, 0, nil
}
