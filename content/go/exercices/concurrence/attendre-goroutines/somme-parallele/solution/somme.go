// Package somme additionne les résultats d'une fonction lente appelée en parallèle.
package somme

import "sync"

// Somme renvoie la somme des f(v), en appelant f en parallèle pour chaque valeur.
func Somme(valeurs []int, f func(int) int) int {
	resultats := make([]int, len(valeurs))
	var wg sync.WaitGroup
	for i, v := range valeurs {
		wg.Go(func() {
			resultats[i] = f(v)
		})
	}
	wg.Wait()

	total := 0
	for _, r := range resultats {
		total += r
	}
	return total
}
