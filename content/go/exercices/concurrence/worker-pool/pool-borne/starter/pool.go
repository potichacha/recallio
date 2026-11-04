// Package pool applique une fonction à des tâches avec un nombre borné de workers.
package pool

// Traiter applique f à chaque tâche avec au plus nWorkers appels simultanés, et renvoie les résultats dans l'ordre.
func Traiter(taches []int, nWorkers int, f func(int) int) []int {
	resultats := make([]int, len(taches))
	for i, t := range taches {
		resultats[i] = f(t)
	}
	return resultats
}
